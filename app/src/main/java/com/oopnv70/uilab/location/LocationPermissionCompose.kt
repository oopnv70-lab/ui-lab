package com.oopnv70.uilab.location

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// =====================================================================
// 定位权限申请：Compose 封装（v2，已修「过一会儿按钮变回授权」bug）
// =====================================================================
// v1 的三个缺陷：
//   1. hasBeenAsked 存内存 → 进程重启归零 → 状态误判为 NOT_REQUESTED
//      → 按钮变回「授权」，但系统已「拒绝且不再询问」→ 点了不弹框。
//   2. 「去设置」按钮其实也调的 requestPermissions()，永远打不开设置页。
//   3. 从系统设置页开完权限回来，界面不会刷新（要重启 App）。
//
// v2 的修法：
//   1. hasBeenAsked 落 SharedPreferences（见 LocationPermissionStore.kt）。
//   2. 暴露 openAppSettings()，DENIED_PERMANENTLY 时由 UI 调它。
//   3. 监听 ON_RESUME，回前台就重算权限状态。
// =====================================================================

/** 从任意 Context 里找到宿主 Activity（申请权限必须有 Activity）。 */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * 记住（并可选自动申请）定位权限。
 *
 * @param autoRequest 是否在首次进入时自动弹出权限申请框。
 * @param onResult 用户完成选择后的回调，参数是解析后的权限状态。
 * @return 当前定位权限状态（可用于 UI 显示）。
 */
@Composable
fun rememberLocationPermission(
    autoRequest: Boolean = true,
    onResult: (LocationPermissionState) -> Unit = {}
): LocationPermissionState {
    val context = LocalContext.current

    // 当前状态：初始值带上持久化的 hasBeenAsked，避免重启后误判为 NOT_REQUESTED。
    var state by remember {
        mutableStateOf(currentLocationPermissionState(context))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // 结果回来后重算（此时 hasBeenAsked 已在申请时落盘）
        val newState = currentLocationPermissionState(context)
        state = newState
        onResult(newState)
        android.util.Log.d(
            "UiLab.Location",
            "permission result=$result -> $newState (${newState.description()})"
        )
    }

    // 自动申请：只在「从未申请过」时触发一次。
    LaunchedEffect(Unit) {
        val current = currentLocationPermissionState(context)
        state = current
        if (autoRequest && current == LocationPermissionState.NOT_REQUESTED) {
            // ⚠️ 关键：申请「之前」就把标记落盘。
            // 若等回调再写，进程在弹窗期间被杀会丢失标记。
            markLocationPermissionAsked(context)
            launcher.launch(LocationPermissions.REQUEST_ARRAY)
        }
    }

    // 从系统设置页返回时刷新状态：
    // 这里刻意【不】用 Lifecycle 观察者 —— 新 Compose（BOM 2026.x）里
    // androidx.compose.ui.platform.LocalLifecycleOwner 已迁移/废弃，
    // 而项目又没引 lifecycle-runtime-compose，用了会编译失败。
    // 改由调用方（MainActivity）在 onResume 时重算并回传，零依赖、零风险。

    return state
}

/**
 * 手动再次申请（例如用户在「城市」页点了「授权」按钮）。
 *
 * 返回一个无参函数，调用即发起申请。
 */
@Composable
fun rememberLocationRequester(
    onResult: (LocationPermissionState) -> Unit = {}
): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val newState = currentLocationPermissionState(context)
        onResult(newState)
        android.util.Log.d(
            "UiLab.Location",
            "manual permission result=$result -> $newState"
        )
    }

    return remember(launcher, context) {
        {
            // 同样：申请前落盘标记
            markLocationPermissionAsked(context)
            launcher.launch(LocationPermissions.REQUEST_ARRAY)
        }
    }
}

/**
 * 判断是否「永久拒绝」（勾了不再询问 / 多次拒绝）。
 *
 * 永久拒绝时再申请系统会静默返回 denied，必须先引导用户去设置页。
 * 需要 Activity 才能调用 shouldShowRequestPermissionRationale。
 */
fun isPermanentlyDenied(context: Context, permission: String): Boolean {
    val activity = context.findActivity() ?: return false
    val granted = ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    return !granted && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}