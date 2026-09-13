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
import com.oopnv70.uilab.ui.weather.CityItem
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
import com.oopnv70.uilab.location.LocateUiState
import com.oopnv70.uilab.location.LocationPermissionState
import com.oopnv70.uilab.location.cityDisplayName
import androidx.compose.ui.graphics.Color

/**
 * 应用主框架（天气版）。
 *
 * 结构：
 *  ┌───────────────────────────────────┐
 *  │  [灵动岛小药丸]  ← 悬浮在最上层      │  ← 点击展开/收起
 *  │                                   │
 *  │      内容区（4 个天气大类页面）      │  ← 随底部导航切换
 *  │                                   │
 *  │  [浮动胶囊导航栏]                  │  ← 概览 / 逐时 / 预报 / 城市
 *  └───────────────────────────────────┘
 *
 * 注意图层顺序：胶囊必须画在内容之后（下层），否则会被内容遮住。
 *
 * @param locationPermissionState 定位权限状态（由 MainActivity 申请后传入）。
 * @param permissionDiagnostics 权限诊断摘要（原始 FINE/COARSE 值 + 判定），
 *        显示在界面上供截图取证，排查「系统说已授权、App 说没权限」。
 * @param locateState 定位结果状态（拿到真实城市名后用于替换写死数据）。
 * @param onRetryLocate 手动重新定位的回调。
 */
@Composable
fun LabApp(
    locationPermissionState: LocationPermissionState = LocationPermissionState.NOT_REQUESTED,
    permissionDiagnostics: String = "",
    locateState: LocateUiState = LocateUiState.Idle,
    onRetryLocate: () -> Unit = {}
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    // 灵动岛是否展开（不跨进程保存，属于「临时 UI 状态」）
    var islandExpanded by remember { mutableStateOf(false) }
    val groups = WeatherGroup.entries

    // 定位到的真实城市名（成功才有值）
    val locatedCity = locateState.cityDisplayName

    // ---- 城市管理状态（提升到这里，CitiesPage 只负责渲染） ----
    // 注意：用 remember 而非 rememberSaveable —— List<String> 的默认实现
    // （Arrays$ArrayList / EmptyList）不是 Serializable，存 Bundle 会崩。
    // 城市选择属于临时 UI 状态，不需要跨进程恢复。
    var extraCityNames by remember { mutableStateOf(listOf<String>()) }
    var removedCityNames by remember { mutableStateOf(listOf<String>()) }
    var selectedCityName by remember { mutableStateOf<String?>(null) }

    // 完整城市列表 = 默认列表（定位城市替换为真实名） - 用户删掉的 + 用户添加的
    val cities: List<CityItem> = remember(locatedCity, extraCityNames, removedCityNames) {
        val base = MockWeather.citiesWith(locatedCity)
            .filter { it.name !in removedCityNames }
        val extras = extraCityNames.mapNotNull { name ->
            MockWeather.allCities.firstOrNull { it.name == name }
        }
        base + extras
    }

    // 当前生效的城市对象：优先「用户手动选中」，否则「定位城市」，再否则列表第一项
    val effectiveCity: CityItem? = remember(cities, selectedCityName, locatedCity) {
        cities.firstOrNull { it.name == selectedCityName }
            ?: cities.firstOrNull { it.isCurrent }
            ?: cities.firstOrNull()
    }

    // 实况数据：跟随当前生效的城市（切换城市时温度/天气真的会变）
    val currentWeather = remember(effectiveCity, cities) {
        MockWeather.currentForCity(effectiveCity, cities)
    }

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
                WeatherGroup.OVERVIEW -> OverviewPage(current = currentWeather)
                WeatherGroup.HOURLY -> HourlyPage()
                WeatherGroup.DAILY -> DailyPage()
                WeatherGroup.CITIES -> CitiesPage(
                    locationPermissionState = locationPermissionState,
                    permissionDiagnostics = permissionDiagnostics,
                    locateState = locateState,
                    cities = cities,
                    selectedCity = effectiveCity?.name,
                    onSelectCity = { city -> selectedCityName = city.name },
                    onAddCity = { city ->
                        if (city.name !in extraCityNames && city.name !in removedCityNames) {
                            extraCityNames = extraCityNames + city.name
                        } else if (city.name in removedCityNames) {
                            // 被删过的默认城市，重新加回来
                            removedCityNames = removedCityNames - city.name
                        }
                    },
                    onRemoveCity = { city ->
                        if (city.name in extraCityNames) {
                            extraCityNames = extraCityNames - city.name
                        } else {
                            removedCityNames = removedCityNames + city.name
                        }
                        // 如果删掉的正是当前选中项，回退到跟随定位
                        if (selectedCityName == city.name) selectedCityName = null
                    },
                    onRetryLocate = onRetryLocate
                )
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
                current = currentWeather,
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