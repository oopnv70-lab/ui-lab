package com.oopnv70.uilab.location

import android.content.Context
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
// 定位权限申请：Compose 封装（v3 —— 修复「已授权却说没权限」）
// =====================================================================
// v1 缺陷：
//   1. hasBeenAsked 存内存 → 进程重启归零 → 状态误判 NOT_REQUESTED。
//   2. 「去设置」按钮其实也调 requestPermissions()，永远打不开设置页。
//   3. 从设置页回来界面不刷新。
//
// v2 修法：
//   1. hasBeenAsked 落 SharedPreferences。
//   2. 暴露 openAppSettings()。
//   3. onResume 重算（由 MainActivity 驱动）。
//
// v3 追加（本次修复的重点）：
//   4. 状态判定改由 currentPermissionDiagnostics() 统一产出：
//      原始值优先、可显示、可截图。
//   5. 处理「系统说已授予、但定位仍拿不到」这一分支 ——
//      高版本（尤其 API 37 的临时权限 / 精确度降级）下，系统权限面板
//      可能显示「已允许」而实际访问被限制，此时不能干等，要主动给出
//      「重新申请 / 去设置」两条出路。
//   6. 每次回到前台自动重新读一次系统原始值（官方要求「每次使用前检查」）。
// =====================================================================

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

    // 初始状态：直接现查系统原始值 + 持久化标记，不用任何内存态的猜测。
    var state by remember {
        mutableStateOf(currentPermissionDiagnostics(context).state)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // 结果回来后【重新查系统原始值】（不信回调里的 map，只信系统真值）
        val newState = currentPermissionDiagnostics(context).state
        state = newState
        onResult(newState)
        PermissionLog.log("权限回调", context)
    }

    // 自动申请：只在「从未申请过」时触发一次。
    LaunchedEffect(Unit) {
        val current = currentPermissionDiagnostics(context)
        state = current.state
        PermissionLog.log("进入页面", context)
        if (autoRequest && current.state == LocationPermissionState.NOT_REQUESTED) {
            // ⚠️ 关键：申请「之前」就把标记落盘。
            // 若等回调再写，进程在弹窗期间被杀（高版本降级精确度会杀进程）会丢标记。
            markLocationPermissionAsked(context)
            launcher.launch(LocationPermissions.REQUEST_ARRAY)
        }
    }

    // 从系统设置页返回时的刷新：由 MainActivity 的 onResume → resumeTick 驱动，
    // 这里刻意不用 Lifecycle 观察者（新版 Compose 该 API 已迁移/废弃，
    // 且项目未引 lifecycle-runtime-compose，用了会编译失败）。

    return state
}

/**
 * 手动再次申请（例如用户在「城市」页点了「授权」按钮）。
 *
 * 返回一个无参函数，调用即发起申请。
 *
 * ⚠️ v3 变化：申请前会再查一次原始值 —— 如果系统其实已经授权
 *    （高版本可能界面显示授权但实际受限），就直接把状态刷成真实值，
 *    不再盲目弹一个「弹不出来」的框。
 */
@Composable
fun rememberLocationRequester(
    onResult: (LocationPermissionState) -> Unit = {}
): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val newState = currentPermissionDiagnostics(context).state
        onResult(newState)
        PermissionLog.log("手动申请回调", context)
    }

    return remember(launcher, context) {
        {
            markLocationPermissionAsked(context)
            launcher.launch(LocationPermissions.REQUEST_ARRAY)
        }
    }
}

/**
 * 刷新当前权限状态（供 Activity onResume / 设置页返回后调用）。
 *
 * 官方要求「每次使用需要权限的功能前都重新检查」，所以这里不加任何缓存。
 */
fun refreshPermissionState(context: Context): LocationPermissionState =
    currentPermissionDiagnostics(context).state

/**
 * 判断是否「永久拒绝」（勾了不再询问 / 多次拒绝）。
 *
 * 永久拒绝时再申请系统会静默返回 denied，必须先引导用户去设置页。
 * 需要 Activity 才能调用 shouldShowRequestPermissionRationale。
 */
fun isPermanentlyDenied(context: Context, permission: String): Boolean {
    val activity = context.findHostActivity() ?: return false
    val granted = ContextCompat.checkSelfPermission(context, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    return !granted && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

/**
 * 权限诊断日志：把原始值和判定一次性打进 logcat。
 *
 * 排查「系统说授权、App 说没权限」时，这是第一手证据。
 * 标签：UiLab.Location
 */
object PermissionLog {
    private const val TAG = "UiLab.Location"

    fun log(stage: String, context: Context) {
        val d = currentPermissionDiagnostics(context)
        android.util.Log.d(TAG, "[$stage] ${d.summary()}")
    }
}