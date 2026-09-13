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
// 定位权限申请：Compose 封装
// =====================================================================
// 用法（在 MainActivity 的 setContent 里）：
//
//     val state = rememberLocationPermission(autoRequest = true) { granted ->
//         // 回调：用户做完选择后触发
//     }
//
// autoRequest = true 时，进入界面会【自动弹一次】系统权限框。
// 这是天气 App 的常见做法（打开就问位置，否则没法给天气）。
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
    // 记录「是否申请过」。真实项目应持久化到 DataStore，
    // 这里用内存状态即可满足当前阶段（重进 App 会重新询问，符合直觉）。
    var hasBeenAsked by remember { mutableStateOf(false) }

    // 当前状态
    var state by remember {
        mutableStateOf(
            getLocationPermissionState(context, hasBeenAsked = false)
        )
    }

    // 结果回调
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasBeenAsked = true
        val newState = getLocationPermissionState(context, hasBeenAsked = true)
        state = newState
        onResult(newState)
        // 调试日志：结果里 FINE / COARSE 各自的授权情况
        android.util.Log.d(
            "UiLab.Location",
            "permission result=$result -> $newState (${newState.description()})"
        )
    }

    // 自动申请：只在「从未申请过」时触发一次。
    // 用 LaunchedEffect(Unit) 保证整个界面生命周期内只跑一次，
    // 避免重组时反复弹窗（那会很烦人）。
    LaunchedEffect(Unit) {
        val current = getLocationPermissionState(context, hasBeenAsked = false)
        state = current
        if (autoRequest && current == LocationPermissionState.NOT_REQUESTED) {
            launcher.launch(LocationPermissions.REQUEST_ARRAY)
        }
    }

    return state
}

/**
 * 手动再次申请（例如用户在「城市」页点了「定位」按钮）。
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
        val newState = getLocationPermissionState(context, hasBeenAsked = true)
        onResult(newState)
        android.util.Log.d(
            "UiLab.Location",
            "manual permission result=$result -> $newState"
        )
    }

    return remember(launcher) {
        {
            launcher.launch(LocationPermissions.REQUEST_ARRAY)
        }
    }
}

/**
 * 判断是否「永久拒绝」（勾了不再询问）。
 * 永久拒绝时，再申请系统会静默返回 denied，必须先引导用户去设置页。
 * 需要 Activity 才能调用 shouldShowRequestPermissionRationale。
 */
fun isPermanentlyDenied(context: Context, permission: String): Boolean {
    val activity = context.findActivity() ?: return false
    val granted = ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    return !granted && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}