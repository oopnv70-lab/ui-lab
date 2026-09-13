package com.oopnv70.uilab.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

// =====================================================================
// 定位权限：状态模型 + 判断逻辑（v3 —— 「高版本已授权却说没权限」修复）
// =====================================================================
//
// 【本版要解决的问题】
//
//   用户现象：杀后台 / 过一段时间后，App 说「没有位置权限」，
//   但打开系统设置 → 应用 → 权限，明明显示「已允许」。
//   点界面上的「授权」又没反应，只有「清除数据」才能恢复。
//
// 【查到的官方依据】（developer.android.com，2026-09 版）
//
//   1) Request runtime permissions 页原文：
//      "在某些情况下，权限可能被自动拒绝，而无需用户做任何操作……
//       重要的是：**不要对自动行为做任何假设**。每次 App 需要访问需要
//       权限的功能时，都要检查是否仍然被授予该权限。"
//      → 结论：权限状态必须【每次现查】，不能靠任何自维护的标记。
//
//   2) 同页："若用户撤销一次性（one-time）权限，App 进程会被终止。"
//   3) 位置权限运行时页：用户把定位从「确切」降级为「大致」时，
//      **系统会重启应用进程**。
//      → 结论：进程随时可能被系统重启，任何只存【内存】的标记都会丢。
//
//   4) Android 17（API 37）新规原文：
//      "If your app targets Android 17 (API level 37) or later and only
//       contains features that require session-based location access to
//       function, Google Play policy requires you to use the location button."
//      配套新权限 USE_LOCATION_BUTTON（Added in API level 37）：
//      "grants **temporary** location permission when a user clicks the button."
//      → 结论：高版本对「只用一次定位」的 App 走的是【临时权限】模型，
//        临时权限会失效，而系统设置面板仍可能显示「已允许」。
//
// 【v3 的修法】
//
//   - `checkSelfPermission` 的原始返回值是【唯一真相】，其余一切（是否申请过、
//     是否永久拒绝）都只是【辅助描述】，绝不允许覆盖原始值。
//   - 状态判定拆成两层：
//       rawState   = 只看系统原始值（granted / not granted）
//       fullState  = rawState + 辅助信息（是否申请过、是否永久拒绝）
//   - 新增 PermissionDiagnostics：把原始值和判定过程一并返回，
//     可以直接显示在界面上，用户截图即可确诊，不用再猜。
// =====================================================================

/** 定位权限的综合状态。 */
enum class LocationPermissionState {
    /** 从未申请过（可以弹窗申请）。 */
    NOT_REQUESTED,

    /** 已授予【精确】位置。 */
    GRANTED_PRECISE,

    /** 已授予，但只有【模糊 / 大致】位置（用户在弹窗里关掉了精确开关）。 */
    GRANTED_APPROXIMATE,

    /** 被拒绝，但还可以再次申请（用户点了"不允许"但没勾选"不再询问"）。 */
    DENIED,

    /** 被永久拒绝（勾了"不再询问"或多次拒绝），只能去系统设置里改。 */
    DENIED_PERMANENTLY;

    /** 是否拿到了任意一种位置权限（精确或模糊）。 */
    val isGranted: Boolean
        get() = this == GRANTED_PRECISE || this == GRANTED_APPROXIMATE
}

/**
 * 权限常量集中定义。
 *
 * 顺序：**FINE 在前，COARSE 在后**（官方示例顺序）。
 *
 * ⚠️ 两者必须【一起申请】：Android 12+ 只声明 / 只申请 FINE 而不带 COARSE 时，
 *    某些系统版本会直接忽略这次请求（官方日志会出现
 *    "ACCESS_FINE_LOCATION must be requested with ACCESS_COARSE_LOCATION"）。
 */
object LocationPermissions {

    /** 精确位置权限。 */
    const val FINE: String = Manifest.permission.ACCESS_FINE_LOCATION

    /** 模糊（大致）位置权限。 */
    const val COARSE: String = Manifest.permission.ACCESS_COARSE_LOCATION

    /** 运行时申请用的权限数组（两个一起，顺序 FINE → COARSE）。 */
    val REQUEST_ARRAY: Array<String> = arrayOf(FINE, COARSE)
}

/**
 * 权限诊断快照：把「系统原始值」和「我们的判定」放在一起。
 *
 * 用途：直接显示在界面上（或打进 log），用户截图即可定位问题，
 *      避免再多轮「猜 → 改 → 试」。
 */
data class PermissionDiagnostics(
    /** ACCESS_FINE_LOCATION 的原始检查结果。 */
    val fineGranted: Boolean,
    /** ACCESS_COARSE_LOCATION 的原始检查结果。 */
    val coarseGranted: Boolean,
    /** 持久化标记：是否申请过。 */
    val hasBeenAsked: Boolean,
    /** shouldShowRequestPermissionRationale(FINE)：true 表示「拒绝过但仍可再问」。 */
    val showRationaleFine: Boolean?,
    /** 是否被判定为「永久拒绝」。 */
    val permanentlyDenied: Boolean,
    /** 最终判定出的状态。 */
    val state: LocationPermissionState,
    /** 系统版本（安卓 17 = API 37）。 */
    val sdkInt: Int
) {
    /** 供界面显示的单行摘要。 */
    fun summary(): String = buildString {
        append("FINE=").append(if (fineGranted) "授予" else "未授予")
        append(" · COARSE=").append(if (coarseGranted) "授予" else "未授予")
        append(" · 问过=").append(if (hasBeenAsked) "是" else "否")
        append(" · rationale=").append(showRationaleFine?.toString() ?: "n/a")
        append(" · 判定=").append(state.name)
        append(" · API=").append(sdkInt)
    }
}

/**
 * 只读系统原始权限值 —— 唯一真相来源。
 *
 * 不做任何推断，不加任何缓存。官方明确要求「每次使用前重新检查」。
 */
fun checkLocationPermissionsRaw(context: Context): Pair<Boolean, Boolean> {
    val fine = ContextCompat.checkSelfPermission(
        context, LocationPermissions.FINE
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context, LocationPermissions.COARSE
    ) == PackageManager.PERMISSION_GRANTED
    return fine to coarse
}

/**
 * 读取当前定位权限状态。
 *
 * @param context 任意 Context。
 * @param hasBeenAsked 调用方是否已经申请过（用于区分"从未申请"和"已被拒绝"）。
 *        这个信息系统 API 拿不到，需要调用方自己持久化记录（SharedPreferences）。
 *        ⚠️ 它只影响 NOT_REQUESTED / DENIED 的区分，**绝不影响 granted 判定**。
 */
fun getLocationPermissionState(
    context: Context,
    hasBeenAsked: Boolean
): LocationPermissionState {
    val (fineGranted, coarseGranted) = checkLocationPermissionsRaw(context)

    return when {
        // 系统说给了，就是给了 —— 优先级最高，任何辅助标记都不能推翻它。
        fineGranted -> LocationPermissionState.GRANTED_PRECISE
        coarseGranted -> LocationPermissionState.GRANTED_APPROXIMATE
        !hasBeenAsked -> LocationPermissionState.NOT_REQUESTED
        // 未授予 + 问过：
        // 是否「永久拒绝」需要 Activity 才能用 rationale 判断，
        // 无 Activity 时保守地给 DENIED（UI 层会用 isPermanentlyDenied 再兜一道）。
        else -> LocationPermissionState.DENIED
    }
}

/**
 * 读取状态 + 完整诊断信息（推荐 UI 层用这个）。
 *
 * 相比 currentLocationPermissionState()，这里额外拿到 rationale，
 * 因此能把 DENIED / DENIED_PERMANENTLY 分得更准。
 */
fun currentPermissionDiagnostics(context: Context): PermissionDiagnostics {
    val (fineGranted, coarseGranted) = checkLocationPermissionsRaw(context)
    val asked = isLocationPermissionAsked(context)

    // rationale 需要 Activity；拿不到就记 null（不代表 false）。
    val rationale: Boolean? = try {
        val activity = context.findHostActivity()
        if (activity == null) null
        else androidx.core.app.ActivityCompat
            .shouldShowRequestPermissionRationale(activity, LocationPermissions.FINE)
    } catch (t: Throwable) {
        null
    }

    // 永久拒绝 = 没授权 + 问过 + 系统明确说「不用再解释了」。
    val permanentlyDenied = !fineGranted && !coarseGranted && asked && rationale == false

    val state = when {
        fineGranted -> LocationPermissionState.GRANTED_PRECISE
        coarseGranted -> LocationPermissionState.GRANTED_APPROXIMATE
        !asked -> LocationPermissionState.NOT_REQUESTED
        permanentlyDenied -> LocationPermissionState.DENIED_PERMANENTLY
        else -> LocationPermissionState.DENIED
    }

    return PermissionDiagnostics(
        fineGranted = fineGranted,
        coarseGranted = coarseGranted,
        hasBeenAsked = asked,
        showRationaleFine = rationale,
        permanentlyDenied = permanentlyDenied,
        state = state,
        sdkInt = Build.VERSION.SDK_INT
    )
}

/** 状态对应的中文说明（用于 UI 展示）。 */
fun LocationPermissionState.description(): String = when (this) {
    LocationPermissionState.NOT_REQUESTED -> "尚未申请定位权限"
    LocationPermissionState.GRANTED_PRECISE -> "已获得精确位置"
    LocationPermissionState.GRANTED_APPROXIMATE -> "仅获得大致位置"
    LocationPermissionState.DENIED -> "定位权限被拒绝"
    LocationPermissionState.DENIED_PERMANENTLY -> "定位权限被永久拒绝，请到系统设置中开启"
}
