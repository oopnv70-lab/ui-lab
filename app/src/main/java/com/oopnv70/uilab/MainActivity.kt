package com.oopnv70.uilab

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.oopnv70.uilab.data.DependencySelfCheck
import com.oopnv70.uilab.location.LocateUiState
import com.oopnv70.uilab.location.LocationPermissionState
import com.oopnv70.uilab.location.currentLocationPermissionState
import com.oopnv70.uilab.location.rememberAutoLocation
import com.oopnv70.uilab.location.rememberLocationPermission
import com.oopnv70.uilab.ui.LabApp
import com.oopnv70.uilab.ui.theme.UiLabTheme

class MainActivity : ComponentActivity() {

    /**
     * 每次回到前台自增一次。
     *
     * 用途：用户被引导去「系统设置」手动开权限，回来后要立刻刷新界面上的
     * 权限状态 —— 否则界面还停在「去设置」，用户以为没生效。
     *
     * 刻意用 Activity 自带的 onResume，而不是 Compose 的 Lifecycle 观察者：
     * 后者在新版 Compose 里 API 已迁移，且项目未引 lifecycle-runtime-compose，
     * 用了有编译风险。onResume 是零依赖、绝对可靠的。
     */
    private val resumeTick = mutableStateOf(0)

    override fun onResume() {
        super.onResume()
        resumeTick.value++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 边到边显示：内容延伸到状态栏/导航栏后面，
        // 浮动胶囊导航栏与灵动岛因此能真正「浮」在内容之上。
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 依赖层自检：确认 kotlinx.serialization 链路在真机上可用。
        // 仅 DEBUG 构建输出，release 下 Log.d 会被编译期剥离。
        if (BuildConfig.DEBUG) {
            Log.d("UiLab", "serialization self-check -> ${DependencySelfCheck.parseSample()}")
        }
        setContent {
            UiLabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // ================= 定位流水线 =================
                    // 两步：
                    //   ① 申请权限（rememberLocationPermission）
                    //   ② 拿到权限后自动定位（rememberAutoLocation）
                    //
                    // 初值必须由 currentLocationPermissionState() 给出 —— 它带上
                    // 了持久化的 hasBeenAsked。以前写死 NOT_REQUESTED，导致 App
                    // 重启后即使系统已"拒绝且不再询问"，界面也显示「授权」，
                    // 点下去却静默无反应。
                    var permissionState by remember {
                        mutableStateOf(currentLocationPermissionState(LocalContext.current))
                    }

                    // onResume 自增（见 Activity 成员 resumeTick），用来触发回到前台后的重算。
                    val tick = resumeTick.value
                    val context = LocalContext.current

                    // 权限状态由 rememberLocationPermission 内部管理并返回；
                    // 它已经带上了持久化的 hasBeenAsked（见该函数实现）。
                    val permissionStateFromHook = rememberLocationPermission(
                        autoRequest = true,
                        onResult = { state ->
                            permissionState = state
                        }
                    )

                    // 两个触发源合并成一个 effect：
                    //   - permissionStateFromHook 变化 → 同步（含首次自动申请的结果）
                    //   - tick 变化（onResume 回到前台）→ 重新读一次（用户可能刚在设置页开了权限）
                    LaunchedEffect(permissionStateFromHook, tick) {
                        permissionState = if (tick == 0) {
                            permissionStateFromHook
                        } else {
                            currentLocationPermissionState(context)
                        }
                    }

                    // 权限一旦授予，这里会自动发起定位并把城市名传下去。
                    val locator = rememberAutoLocation(
                        permissionState = permissionState,
                        enabled = true
                    )

                    LabApp(
                        locationPermissionState = permissionState,
                        locateState = locator.state,
                        onRetryLocate = locator.refresh
                    )
                }
            }
        }
    }
}