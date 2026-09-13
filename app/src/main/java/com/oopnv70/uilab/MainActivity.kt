package com.oopnv70.uilab

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.oopnv70.uilab.data.DependencySelfCheck
import com.oopnv70.uilab.location.LocationPermissionState
import com.oopnv70.uilab.location.rememberLocationPermission
import com.oopnv70.uilab.ui.LabApp
import com.oopnv70.uilab.ui.theme.UiLabTheme

class MainActivity : ComponentActivity() {

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
                    // ---------- 定位权限申请 ----------
                    // autoRequest = true：进入 App 时自动弹一次系统权限框。
                    // 这是天气 App 的标准做法 —— 拿不到位置就没法给天气。
                    //
                    // ⚠️ 申请的是 FINE + COARSE 两个权限：
                    //    Android 12+ 只有两个一起申请，弹窗里才会出现
                    //    「精确位置」开关，用户才有机会授权精确位置。
                    var locationState by remember {
                        mutableStateOf(LocationPermissionState.NOT_REQUESTED)
                    }

                    rememberLocationPermission(
                        autoRequest = true,
                        onResult = { state ->
                            locationState = state
                        }
                    )

                    LabApp(
                        locationPermissionState = locationState
                    )
                }
            }
        }
    }
}