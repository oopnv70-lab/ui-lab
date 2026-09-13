package com.oopnv70.uilab.location

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

// =====================================================================
// 定位状态：Compose 封装
// =====================================================================
// 职责：把「权限状态 + 定位结果」串成一条自动流水线。
//
//   permissionState 变化
//        ↓（自动）
//     授予 → 自动发起定位 → 拿到 LocatedPlace → UI 更新城市名
//     拒绝 → 状态设为 FAILED，UI 提示去授权
//
// 这是修复「打开 App 显示北京」这个 bug 的关键：
//   以前只有权限申请，没有「申请到了去定位」这一步。
// =====================================================================

/** 定位的整体状态。 */
sealed interface LocateUiState {
    /** 还没开始（例如等待权限）。 */
    data object Idle : LocateUiState
    /** 正在定位（[stage] 区分"取坐标"还是"反解地名"）。 */
    data class Locating(val stage: LocateStage) : LocateUiState
    /** 定位成功。 */
    data class Success(val place: LocatedPlace) : LocateUiState
    /** 定位失败（无权限 / 无信号 / 超时）。 */
    data class Failed(val reason: String) : LocateUiState
}

/**
 * 自动定位：权限一授予就发起定位，拿到结果后存入状态。
 *
 * @param permissionState 外部传入的当前权限状态（来自 rememberLocationPermission）。
 * @param enabled 是否启用（默认 true）。设 false 可以暂停自动定位。
 * @return 定位 UI 状态；[refresh] 可供 UI 手动重试。
 */
@Composable
fun rememberAutoLocation(
    permissionState: LocationPermissionState,
    enabled: Boolean = true
): LocateController {
    val context = LocalContext.current

    var state by remember { mutableStateOf<LocateUiState>(LocateUiState.Idle) }

    // 手动触发用的「重试计数器」：自增一次 = 重新定位一次。
    var retryTick by remember { mutableStateOf(0) }

    // 关键：监听权限状态。一旦「已授予」，就开始定位。
    // 用 retryTick 作为 key 的一部分，让"重试"也能重新触发。
    LaunchedEffect(permissionState, retryTick, enabled) {
        if (!enabled) return@LaunchedEffect

        when {
            permissionState.isGranted -> {
                Log.d("UiLab.Location", "权限已授予，开始自动定位 (retry=$retryTick)")

                val place = locateCurrentPlace(context) { stage ->
                    state = LocateUiState.Locating(stage)
                }

                state = if (place != null) {
                    Log.d("UiLab.Location", "定位成功: ${place.displayName} (${place.latitude}, ${place.longitude})")
                    LocateUiState.Success(place)
                } else {
                    Log.w("UiLab.Location", "定位失败")
                    LocateUiState.Failed("定位失败，请检查定位服务是否开启")
                }
            }

            permissionState == LocationPermissionState.DENIED_PERMANENTLY -> {
                state = LocateUiState.Failed("定位权限被永久拒绝，请到系统设置中开启")
            }

            permissionState == LocationPermissionState.DENIED -> {
                state = LocateUiState.Failed("未获得定位权限")
            }

            else -> {
                // NOT_REQUESTED：等权限弹窗结果，什么都不做
                state = LocateUiState.Idle
            }
        }
    }

    return remember(state, retryTick) {
        LocateController(
            state = state,
            refresh = { retryTick++ }
        )
    }
}

/** 定位控制器：状态 + 重试入口。 */
data class LocateController(
    /** 当前定位状态。 */
    val state: LocateUiState,
    /** 手动重新定位（例如用户点了「重试」按钮）。 */
    val refresh: () -> Unit
)

/** 便捷扩展：从状态里取城市显示名（没有就返回 null）。 */
val LocateUiState.cityDisplayName: String?
    get() = (this as? LocateUiState.Success)?.place?.displayName

/** 便捷扩展：是否正在定位中。 */
val LocateUiState.isBusy: Boolean
    get() = this is LocateUiState.Locating