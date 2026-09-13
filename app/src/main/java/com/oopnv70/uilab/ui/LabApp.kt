package com.oopnv70.uilab.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oopnv70.uilab.ui.components.FloatingPillNavigationBar
import com.oopnv70.uilab.ui.components.NavItem
import com.oopnv70.uilab.ui.weather.CitiesPage
import com.oopnv70.uilab.ui.weather.DailyPage
import com.oopnv70.uilab.ui.weather.DynamicIslandCapsule
import com.oopnv70.uilab.ui.weather.HourlyPage
import com.oopnv70.uilab.ui.weather.MockWeather
import com.oopnv70.uilab.ui.weather.OverviewPage
import com.oopnv70.uilab.ui.weather.WeatherGroup
import com.oopnv70.uilab.ui.weather.cloudIcon
import com.oopnv70.uilab.ui.weather.pressureIcon
import com.oopnv70.uilab.ui.weather.sunIcon
import com.oopnv70.uilab.ui.weather.sunriseIcon
import androidx.compose.ui.graphics.Color

/**
 * 应用主框架（天气版）。
 *
 * 结构：
 *  ┌───────────────────────────────────┐
 *  │  [灵动岛胶囊]  ← 悬浮在最上层        │  ← 收起/展开
 *  │                                   │
 *  │      内容区（4 个天气大类页面）      │  ← 随底部导航切换
 *  │                                   │
 *  │  [浮动胶囊导航栏]                  │  ← 概览 / 逐时 / 预报 / 城市
 *  └───────────────────────────────────┘
 *
 * 注意图层顺序：胶囊必须画在内容之后（下层），否则会被内容遮住。
 */
@Composable
fun LabApp() {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    // 灵动岛是否展开（不跨进程保存，属于「临时 UI 状态」）
    var islandExpanded by remember { mutableStateOf(false) }

    val groups = WeatherGroup.entries

    // 系统栏避让
    val statusBarTop = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val navBottomInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
        .coerceAtLeast(16.dp)

    Box(modifier = Modifier.fillMaxSize()) {

        // ---------- 内容区 ----------
        AnimatedContent(
            targetState = selectedIndex,
            transitionSpec = {
                fadeIn(tween(220)) togetherWith fadeOut(tween(140))
            },
            label = "weatherTabContent"
        ) { index ->
            when (groups[index]) {
                WeatherGroup.OVERVIEW -> OverviewPage()
                WeatherGroup.HOURLY -> HourlyPage()
                WeatherGroup.DAILY -> DailyPage()
                WeatherGroup.CITIES -> CitiesPage()
            }
        }

        // ---------- 底部：浮动胶囊导航栏 ----------
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = navBottomInset + 8.dp
                )
        ) {
            FloatingPillNavigationBar(
                items = remember { buildNavItems() },
                selectedIndex = selectedIndex,
                onSelect = { selectedIndex = it }
            )
        }

        // ---------- 顶部：灵动岛胶囊（最后画 → 层级最高） ----------
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = statusBarTop + 6.dp
                )
        ) {
            DynamicIslandCapsule(
                current = MockWeather.current,
                expanded = islandExpanded,
                onToggle = { islandExpanded = !islandExpanded }
            )
        }
    }
}

/**
 * 构建导航项：四个天气大类，使用自绘图标。
 * 颜色定义为「未选中色」，实际着色由导航栏按选中状态覆盖。
 */
private fun buildNavItems(): List<NavItem> {
    val tint = Color.Unspecified
    return listOf(
        NavItem(icon = sunIcon(tint), label = WeatherGroup.OVERVIEW.label),
        NavItem(icon = sunriseIcon(tint), label = WeatherGroup.HOURLY.label),
        NavItem(icon = cloudIcon(tint), label = WeatherGroup.DAILY.label),
        NavItem(icon = pressureIcon(tint), label = WeatherGroup.CITIES.label)
    )
}