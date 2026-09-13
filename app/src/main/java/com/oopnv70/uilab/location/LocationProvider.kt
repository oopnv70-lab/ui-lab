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

/**
 * 一次定位的结果：坐标 + 从「省」到「街道」的分级地名。
 *
 * 分级说明（对应 Android [Address] 的字段名，全部可为 null —— 拿不到就留空，
 * 绝不编造）：
 *   province  省 / 自治区      ← Address.adminArea
 *   city      市              ← Address.locality（直辖市时可能为空）
 *   district  区 / 县          ← Address.subLocality（**街道级精度的关键**）
 *   street    街道 / 道路      ← Address.thoroughfare
 *   feature   门牌号 / 具体地点 ← Address.featureName（多数情况下为空）
 *
 * 现实约束（必须知道，否则会误以为代码写坏了）：
 *   这些字段能不能拿到，**完全取决于设备自带的逆地理编码服务**。
 *   国内无 GMS 的机型，Geocoder 常常只给到「市」一级，区的信息可能为空；
 *   有 GMS 或厂商自带地图服务的机型，可以拿到区甚至街道。
 *   所以 UI 必须能优雅地逐级降级显示，而不是假设一定有区/街道。
 */
data class LocatedPlace(
    /** 纬度。 */
    val latitude: Double,
    /** 经度。 */
    val longitude: Double,
    /** 省 / 自治区（如「安徽省」）；拿不到为 null。 */
    val province: String? = null,
    /** 市（如「淮南市」）；拿不到为 null。 */
    val city: String? = null,
    /** 区 / 县（如「田家庵区」）；拿不到为 null。 */
    val district: String? = null,
    /** 街道 / 道路（如「洞山街道」）；拿不到为 null。 */
    val street: String? = null,
    /** 门牌 / 具体地点；拿不到为 null。 */
    val feature: String? = null,
    /** 定位来源，便于调试。 */
    val provider: String
) {
    /**
     * 展示用的一行文字，**从最细的一级往粗降级**。
     *
     * 拼装顺序：省 + 市 + 区 + 街道（能拿到几级就显示几级）。
     * 例：
     *   全部拿到   → 安徽省淮南市田家庵区洞山街道
     *   只到区     → 安徽省淮南市田家庵区
     *   只到市     → 安徽省淮南市
     *   只有省     → 安徽省
     *   全都没有   → 当前定位（绝不用假城市名兜底）
     *
     * 注意：直辖市的 Address.locality 常为空，所以 city 为空时跳过，
     * 不会拼出「北京市北京市」这种重复。
     */
    val displayName: String
        get() {
            val parts = listOfNotNull(
                province?.takeIf { it.isNotBlank() },
                city?.takeIf { it.isNotBlank() },
                district?.takeIf { it.isNotBlank() },
                street?.takeIf { it.isNotBlank() }
            ).distinct()   // 防止「上海市上海市」这类相邻重复
            return if (parts.isEmpty()) "当前定位" else parts.joinToString("")
        }

    /**
     * 精度等级：拿到的**最细一级**是什么。
     *
     * 用于判断当前定位"够不够细"，也方便 UI 明确告诉用户
     * "已定位到街道"还是"只到市区"。
     */
    val precisionLevel: PrecisionLevel
        get() = when {
            !feature.isNullOrBlank() -> PrecisionLevel.FEATURE
            !street.isNullOrBlank() -> PrecisionLevel.STREET
            !district.isNullOrBlank() -> PrecisionLevel.DISTRICT
            !city.isNullOrBlank() -> PrecisionLevel.CITY
            !province.isNullOrBlank() -> PrecisionLevel.PROVINCE
            else -> PrecisionLevel.UNKNOWN
        }
}

/** 定位精度等级，从细到粗。 */
enum class PrecisionLevel(val label: String) {
    /** 门牌 / 具体地点。 */
    FEATURE("具体地点"),
    /** 街道 / 道路。 */
    STREET("街道"),
    /** 区 / 县。 */
    DISTRICT("区县"),
    /** 市。 */
    CITY("城市"),
    /** 省。 */
    PROVINCE("省份"),
    /** 什么都没拿到。 */
    UNKNOWN("未知")
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

    // ---------- 3. 反解地名（省/市/区/街道，能拿到几级算几级） ----------
    onStage(LocateStage.REVERSE_GEOCODING)
    val address = reverseGeocode(appContext, location.latitude, location.longitude)

    onStage(LocateStage.DONE)
    return LocatedPlace(
        latitude = location.latitude,
        longitude = location.longitude,
        // 逐级映射。注意几个易踩的点：
        //   · adminArea     = 省（「安徽省」）
        //   · locality      = 市（「淮南市」）；**直辖市常为空**，此时用 subAdminArea 兜
        //   · subLocality   = 区（「田家庵区」）—— 街道精度的关键字段
        //   · thoroughfare  = 街道 / 道路（「洞山街道」）
        //   · featureName   = 门牌 / 地点名，多数返回为空
        // 每个字段都先 trim + 去空串，避免拿到 "" 让 UI 显示成空白。
        province = address?.adminArea.clean(),
        city = (address?.locality ?: address?.subAdminArea).clean(),
        district = address?.subLocality.clean(),
        street = address?.thoroughfare.clean(),
        feature = address?.featureName.clean(),
        provider = location.provider ?: "unknown"
    )
}

/** 把可能为 null / 空白 / 仅空格的字符串规整成 null，避免 UI 显示空。 */
private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

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
                .getFromLocation(latitude, longitude, 3)
            // 逐条打日志：方便在真机上直接看出「这台设备到底给了哪几级」。
            // 不同厂商的 Geocoder 返回条数和字段完整度差异极大，
            // 有了这些日志就能判断"街道级拿不到"是代码问题还是服务问题。
            list?.forEachIndexed { i, a ->
                Log.d(
                    TAG,
                    "逆地理[$i] adminArea=${a.adminArea} locality=${a.locality} " +
                        "subAdminArea=${a.subAdminArea} subLocality=${a.subLocality} " +
                        "thoroughfare=${a.thoroughfare} featureName=${a.featureName}"
                )
            }
            // 取「信息量最大」的一条，而不是盲取第一条：
            // 有些设备第一条只有省市，第二条才带区 —— 谁更细用谁。
            // 空列表时 maxByOrNull 自然返回 null，无需再兜一次。
            list?.maxByOrNull { a ->
                listOf(a.subLocality, a.thoroughfare, a.featureName)
                    .count { !it.isNullOrBlank() }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "逆地理编码抛异常: ${t.message}")
            null
        }
    }
}

private const val TAG = "UiLab.Location"
