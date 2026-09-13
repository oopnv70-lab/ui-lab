package com.oopnv70.uilab

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.oopnv70.uilab.data.DependencySelfCheck
import com.oopnv70.uilab.ui.LabApp
import com.oopnv70.uilab.ui.theme.UiLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 边到边显示：内容延伸到状态栏/导航栏后面，
        // 浮动胶囊导航栏因此能真正「浮」在内容之上。
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
                    LabApp()
                }
            }
        }
    }
}