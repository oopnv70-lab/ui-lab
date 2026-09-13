package com.oopnv70.uilab.location

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

// =====================================================================
// 权限申请的「已问过」标记：持久化
// =====================================================================
// 为什么必须持久化（这就是「过一段时间按钮又变回授权」的根因）：
//
//   之前用 `remember { mutableStateOf(false) }` 记录 hasBeenAsked，
//   这是【内存态】。App 被系统杀掉 / 重启后归零 →
//   getLocationPermissionState(ctx, hasBeenAsked = false) →
//   返回 NOT_REQUESTED（"尚未申请"）→ 按钮变回「授权」。
//
//   但系统层面其实早就问过了（甚至"拒绝且不再询问"），
//   于是再点「授权」→ requestPermissions() 静默返回 → 看起来"点了没反应"。
//
// 修复：把「问过没有」落到 SharedPreferences，跨进程存活。
// 零新增依赖（不用引 DataStore，SharedPreferences 系统自带、够用）。
// =====================================================================

/** SharedPreferences 文件名。 */
private const val PREFS_NAME = "ui_lab_location"

/** 键：是否已经向用户申请过定位权限。 */
private const val KEY_HAS_BEEN_ASKED = "location_permission_asked"

/** 读取「是否申请过定位权限」。 */
fun isLocationPermissionAsked(context: Context): Boolean =
    context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_HAS_BEEN_ASKED, false)

/** 记住「已经申请过定位权限」。申请那一刻就该写，不能等结果回调。 */
fun markLocationPermissionAsked(context: Context) {
    context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_HAS_BEEN_ASKED, true)
        .apply()
}

/**
 * 读取当前权限状态（自动带上持久化的 hasBeenAsked）。
 *
 * 这是 UI 层唯一应该调用的入口 —— 不要再手工传 hasBeenAsked = false，
 * 那正是之前把状态误判成 NOT_REQUESTED 的原因。
 *
 * ⚠️ v3 起：本函数只做「原始值 + 标记」的合并，判定优先级永远是
 *    `checkSelfPermission` 的原始值最高（见 LocationPermission.kt）。
 */
fun currentLocationPermissionState(context: Context): LocationPermissionState =
    getLocationPermissionState(
        context = context,
        hasBeenAsked = isLocationPermissionAsked(context)
    )

/**
 * 从任意 Context 里找到宿主 Activity。
 *
 * 申请权限、读 shouldShowRequestPermissionRationale 都必须有 Activity。
 * 本函数被 LocationPermission.kt 与 LocationPermissionCompose.kt 共用，
 * 所以放在这里（公开，不 private）。
 */
fun Context.findHostActivity(): android.app.Activity? {
    var ctx: Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * 跳转到本应用的系统设置页（用于「永久拒绝」后引导用户手动开启权限）。
 *
 * 永久拒绝后 requestPermissions() 是不弹框的，只能走这里。
 * 常见降级：极少数设备没有应用详情页，则退回「设置首页」。
 */
fun openAppSettings(context: Context) {
    val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(details)
    } catch (e: Exception) {
        // 个别 ROM 没有应用详情页，退回系统设置首页
        val home = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(home)
        } catch (_: Exception) {
            // 连设置都打不开：无解，静默（日志已足够定位问题）
            android.util.Log.w("UiLab.Location", "无法打开系统设置页", e)
        }
    }
}