package com.oopnv70.uilab

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.oopnv70.uilab.data.DependencySelfCheck
import com.oopnv70.uilab.location.LocationPermissionState
import com.oopnv70.uilab.location.PermissionDiagnostics
import com.oopnv70.uilab.location.PermissionLog
import com.oopnv70.uilab.location.currentPermissionDiagnostics
import com.oopnv70.uilab.location.rememberAutoLocation
import com.oopnv70.uilab.location.rememberLocationPermission
import com.oopnv70.uilab.settings.ThemeMode
import com.oopnv70.uilab.settings.persistAppSettings
import com.oopnv70.uilab.settings.readAppSettings
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
        // 官方要求：每次回到前台都重新读一次系统原始权限值，不做任何缓存假设。
        PermissionLog.log("onResume", this)
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
            // ================= 应用设置 =================
            // 设置读取放在最外层：主题模式与动态取色要决定整个 UiLabTheme，
            // 不能在 Theme 内部读（那就成了「主题决定主题」的循环依赖）。
            //
            // appSettings 是「当前设置」的唯一 UI 真相来源（Activity 级 state）。
            // 设置页改完 → 写 SharedPreferences + 更新这个 state →
            // 立即触发重组 → 主题/单位当场生效，无需重启。
            //
            // ⚠️ 变量名刻意用 activityContext 而不是 context：
            //    下面定位流水线里已经有一个 `val context = LocalContext.current`，
            //    同名会编译失败（重复声明）。
            val activityContext = this@MainActivity
            var appSettings by remember { mutableStateOf(readAppSettings(activityContext)) }

            UiLabTheme(
                darkTheme = when (appSettings.themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
                dynamicColor = appSettings.dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // ================= 定位流水线 =================
                    // 两步：① 申请权限  ② 拿到权限后自动定位
                    //
                    // ⚠️ v3 关键：这里保存的是【完整诊断快照】而不只是枚举。
                    //    原因是「系统说已授权、App 说没权限」这类 bug，
                    //    光看枚举值分不清是「真没权限」还是「权限标记异常」，
                    //    把 FINE/COARSE 原始值一起带上并显示在界面上，
                    //    用户截图一次就能确诊，不用再来回猜。
                    // ⚠️ 必须先取 context，再进 remember：LocalContext 是
                    //    @Composable 属性，不能在 remember { } 的普通 lambda 里调用，
                    //    否则编译报错（这正是上一版 CI 失败的原因）。
                    val context = LocalContext.current
                    val tick = resumeTick.value

                    var diag by remember(context) {
                        mutableStateOf(currentPermissionDiagnostics(context))
                    }

                    // 权限状态由 rememberLocationPermission 内部管理并返回。
                    val permissionStateFromHook = rememberLocationPermission(
                        autoRequest = true,
                        onResult = { state ->
                            // 回调只给枚举，这里补一次完整诊断（含原始值）
                            diag = currentPermissionDiagnostics(context)
                            if (diag.state != state) {
                                Log.w(
                                    "UiLab.Location",
                                    "枚举不一致：hook=$state diag=${diag.state}"
                                )
                            }
                        }
                    )

                    // 两个触发源合并：
                    //   - permissionStateFromHook 变化 → 重查诊断
                    //   - tick 变化（onResume 回到前台）→ 重查诊断
                    LaunchedEffect(permissionStateFromHook, tick) {
                        diag = currentPermissionDiagnostics(context)
                    }

                    // 权限一旦授予，这里会自动发起定位并把城市名传下去。
                    val locator = rememberAutoLocation(
                        permissionState = diag.state,
                        enabled = true
                    )

                    LabApp(
                        locationPermissionState = diag.state,
                        permissionDiagnostics = diag.summary(),
                        locateState = locator.state,
                        onRetryLocate = locator.refresh,
                        appSettings = appSettings,
                        onSettingsChange = { updated ->
                            // 先落盘（持久化），再更新 state（立即生效）。
                            // 顺序刻意为「先写盘」：万一进程被杀，盘上是新的，
                            // 不会出现「界面显示新主题、重启后变回旧主题」。
                            persistAppSettings(activityContext, updated)
                            appSettings = updated
                        }
                    )
                }
            }
        }
    }
}