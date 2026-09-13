package com.oopnv70.uilab.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

// =====================================================================
// 定位层：拿到经纬度 → 反解成城市名
// =====================================================================
// 设计原则：
//   1. 不引入 Google Play Services（FusedLocationProvider）。
//      原因：体积大、国内多数设备没有 GMS、天气 App 不需要那么高精度。
//      用系统自带的 LocationManager 足够。
//   2. 三级降级，尽量拿到结果：
//        ① getCurrentLocation（一次定位，新 API，最准）
//        ② getLastKnownLocation（读缓存，最快，可能旧）
//        ③ 都失败 → 返回 null，UI 显示「定位失败」
//   3. 反解城市名用系统 Geocoder（可能返回英文/为空），
//      失败时降级显示「当前位置」+ 经纬度，绝不伪造城市名。
// =====================================================================

/** 一次定位的结果：坐标 + 城市名（城市名可能为 null）。 */
data class LocatedPlace(
    /** 纬度。 */
    val latitude: Double,
    /** 经度。 */
    val longitude: Double,
    /** 城市名（如「深圳市」）；反解失败时为 null。 */
    val cityName: String?,
    /** 省份 / 上级行政区（如「广东省」）；反解失败时为 null。 */
    val adminName: String?,
    /** 定位来源，便于调试。 */
    val provider: String
) {
    /** 展示用的一行文字：优先「省 + 市」，退化到「当前定位」，再退化到坐标。 */
    val displayName: String
        get() = when {
            cityName != null && adminName != null -> "$adminName$cityName"
            cityName != null -> cityName
            else -> "当前定位"
        }
}

/** 定位过程的阶段（UI 可据此显示不同文案）。 */
enum class LocateStage {
    /** 正在获取坐标。 */
    LOCATING,
    /** 拿到坐标，正在反解城市名。 */
    REVERSE_GEOCODING,
    /** 完成。 */
    DONE,
    /** 失败（无权限 / 无 provider / 超时）。 */
    FAILED
}

/**
 * 获取当前位置。
 *
 * 这是整个定位功能的唯一入口。内部自己处理：
 *   - 权限检查（没权限直接返回 null）
 *   - 三级降级（current → lastKnown）
 *   - 超时保护（不会无限等）
 *   - 逆地理编码（坐标 → 城市名）
 *
 * @param context 任意 Context（内部取 applicationContext）。
 * @param onStage 阶段回调，用于 UI 显示进度。可能在工作线程调用。
 * @return 定位成功返回 [LocatedPlace]；失败返回 null。
 */
@SuppressLint("MissingPermission")
suspend fun locateCurrentPlace(
    context: Context,
    onStage: (LocateStage) -> Unit = {}
): LocatedPlace? {
    val appContext = context.applicationContext

    // ---------- 1. 权限检查 ----------
    // 这里必须自己再查一遍：UI 层传进来的可能是「已授权」，
    // 但也可能用户在系统设置里撤销了。拿到 null 比崩溃好。
    val hasFine = ContextCompat.checkSelfPermission(
        appContext, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        appContext, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) {
        Log.d(TAG, "locate: 没有定位权限，直接返回 null")
        onStage(LocateStage.FAILED)
        return null
    }

    val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (lm == null) {
        Log.w(TAG, "locate: LocationManager 不可用")
        onStage(LocateStage.FAILED)
        return null
    }

    // ---------- 2. 拿坐标 ----------
    onStage(LocateStage.LOCATING)
    val mainExecutor = ContextCompat.getMainExecutor(appContext)
    val location = getLocation(lm, mainExecutor)

    if (location == null) {
        Log.w(TAG, "locate: 所有 provider 都没拿到坐标")
        onStage(LocateStage.FAILED)
        return null
    }
    Log.d(TAG, "locate: 坐标 ${location.latitude}, ${location.longitude} via ${location.provider}")

    // ---------- 3. 反解城市名 ----------
    onStage(LocateStage.REVERSE_GEOCODING)
    val address = reverseGeocode(appContext, location.latitude, location.longitude)

    onStage(LocateStage.DONE)
    return LocatedPlace(
        latitude = location.latitude,
        longitude = location.longitude,
        cityName = address?.locality ?: address?.subAdminArea,
        adminName = address?.adminArea,
        provider = location.provider ?: "unknown"
    )
}

/**
 * 三级降级地拿一次坐标。
 *
 * 顺序：
 *   ① getCurrentLocation —— API 30+，一次定位，最准，但要等几秒
 *   ② getLastKnownLocation —— 读系统缓存，毫秒级，但可能是几小时前的
 *
 * 注意：没有 GPS 信号（在室内）时 ① 会超时，所以给 8 秒上限。
 */
@SuppressLint("MissingPermission")
private suspend fun getLocation(
    lm: LocationManager,
    mainExecutor: java.util.concurrent.Executor
): Location? {
    // ---------- ① getCurrentLocation（新 API） ----------
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val fresh = withTimeoutOrNull(8_000L) {
            suspendCancellableCoroutine { cont ->
                try {
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    lm.getCurrentLocation(
                        LocationManager.NETWORK_PROVIDER,
                        signal,
                        mainExecutor
                    ) { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "getCurrentLocation 抛异常: ${t.message}")
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
        if (fresh != null) return fresh
        Log.d(TAG, "getCurrentLocation 超时或为空，降级到 lastKnown")
    }

    // ---------- ② getLastKnownLocation（读缓存） ----------
    val providers = listOf(
        LocationManager.NETWORK_PROVIDER,
        LocationManager.GPS_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )
    var best: Location? = null
    for (p in providers) {
        val loc = try {
            if (lm.isProviderEnabled(p)) lm.getLastKnownLocation(p) else null
        } catch (t: Throwable) {
            Log.w(TAG, "getLastKnownLocation($p) 抛异常: ${t.message}")
            null
        }
        if (loc != null && (best == null || loc.time > best!!.time)) {
            best = loc
        }
    }
    return best
}

/**
 * 坐标 → 地址。
 *
 * ⚠️ 现实情况（必须说清楚）：
 *   - Geocoder 的逆地理编码**依赖系统自带的地图服务**。
 *   - 国内很多机型（尤其没装 GMS 的）会返回空列表，或只返回英文。
 *   - API 33+ 虽然新增了异步版 getFromLocation(lat,lng,max,listener)，
 *     但 GeocodeListener.onGeocode 的参数类型在 API 33 是
 *     MutableList<Address>、API 34 起是 List<Address>，
 *     不同 compileSdk 下重写会编译失败 —— 非常容易踩坑。
 *
 * 所以这里【统一走阻塞版 getFromLocation(lat, lng, max)】：
 *   - 该重载从 API 1 就存在，签名永不变，兼容所有版本
 *   - 切到 Dispatchers.IO 执行，绝不阻塞主线程
 *   - 整体加 5 秒超时
 *   - 失败返回 null（上层降级显示「当前定位」）
 */
private suspend fun reverseGeocode(
    context: Context,
    latitude: Double,
    longitude: Double
): Address? = withTimeoutOrNull(5_000L) {
    withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            val list = Geocoder(context, Locale.CHINA)
                .getFromLocation(latitude, longitude, 1)
            list?.firstOrNull()
        } catch (t: Throwable) {
            Log.w(TAG, "逆地理编码抛异常: ${t.message}")
            null
        }
    }
}

private const val TAG = "UiLab.Location"
