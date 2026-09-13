package com.oopnv70.uilab.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

// =====================================================================
// 定位权限：状态模型 + 判断逻辑
// =====================================================================
// 关于 Android 12+ 的「精确 / 模糊」两套定位（重点）：
//
//   Android 12（API 31）起，系统弹窗变成「精确位置」开关样式：
//     ┌───────────────────────────────┐
//     │  允许"XX"访问您的位置？         │
//     │  ○ 精确位置                    │
//     │  ○ 大致位置                    │
//     │  [不允许]  [允许]  [仅这一次]   │
//     └───────────────────────────────┘
//
//   ⚠️ 关键：只有【同时】在 Manifest 声明 FINE + COARSE，
//      并【同时】在运行时申请这两个权限，
//      系统才会弹出带「精确位置」开关的那个对话框。
//      只声明 COARSE → 弹窗里根本没有「精确位置」选项，用户无从授权。
//
//   用户在弹窗里关掉「精确位置」开关 → 授权结果变成「仅模糊」。
// =====================================================================

/** 定位权限的综合状态。 */
enum class LocationPermissionState {
    /** 从未申请过（可以弹窗申请）。 */
    NOT_REQUESTED,

    /** 已授予【精确】位置。 */
    GRANTED_PRECISE,

    /** 已授予，但只有【模糊】位置（用户在弹窗里关掉了精确开关）。 */
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
 * 顺序很重要：**FINE 在前，COARSE 在后**。
 * 这是官方文档与示例的推荐顺序，某些系统版本对申请数组的顺序敏感。
 */
object LocationPermissions {

    /** 精确位置权限。 */
    const val FINE: String = Manifest.permission.ACCESS_FINE_LOCATION

    /** 模糊位置权限。 */
    const val COARSE: String = Manifest.permission.ACCESS_COARSE_LOCATION

    /**
     * 运行时申请用的权限数组。
     *
     * ⚠️ 必须两个一起申请，Android 12+ 才会显示「精确位置」开关。
     */
    val REQUEST_ARRAY: Array<String> = arrayOf(FINE, COARSE)
}

/**
 * 读取当前定位权限状态。
 *
 * @param context 任意 Context。
 * @param hasBeenAsked 调用方是否已经申请过（用于区分"从未申请"和"已被拒绝"）。
 *        这个信息系统 API 拿不到，需要调用方自己用 DataStore / SharedPreferences 记录。
 */
fun getLocationPermissionState(
    context: Context,
    hasBeenAsked: Boolean
): LocationPermissionState {
    val fineGranted = ContextCompat.checkSelfPermission(
        context, LocationPermissions.FINE
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context, LocationPermissions.COARSE
    ) == PackageManager.PERMISSION_GRANTED

    return when {
        fineGranted -> LocationPermissionState.GRANTED_PRECISE
        coarseGranted -> LocationPermissionState.GRANTED_APPROXIMATE
        hasBeenAsked -> LocationPermissionState.DENIED
        else -> LocationPermissionState.NOT_REQUESTED
    }
}

/** 状态对应的中文说明（用于 UI 展示）。 */
fun LocationPermissionState.description(): String = when (this) {
    LocationPermissionState.NOT_REQUESTED -> "尚未申请定位权限"
    LocationPermissionState.GRANTED_PRECISE -> "已获得精确位置"
    LocationPermissionState.GRANTED_APPROXIMATE -> "仅获得大致位置"
    LocationPermissionState.DENIED -> "定位权限被拒绝"
    LocationPermissionState.DENIED_PERMANENTLY -> "定位权限被永久拒绝，请到系统设置中开启"
}