package com.oopnv70.uilab.ui

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oopnv70.uilab.glass.GlassDefaults
import com.oopnv70.uilab.glass.GlassFeature
import com.oopnv70.uilab.glass.LiquidGlassSurface
import com.oopnv70.uilab.location.LocateUiState
import com.oopnv70.uilab.location.LocationPermissionState
import com.oopnv70.uilab.settings.AppSettingsState
import com.oopnv70.uilab.settings.ThemeStyle
import com.oopnv70.uilab.ui.components.FloatingPillNavigationBar
import com.oopnv70.uilab.ui.components.NavItem
import com.oopnv70.uilab.ui.settings.SettingsPage
import com.oopnv70.uilab.ui.weather.CitiesPage
import com.oopnv70.uilab.ui.weather.CityItem
import com.oopnv70.uilab.ui.weather.CurrentWeather
import com.oopnv70.uilab.ui.weather.DailyPage
import com.oopnv70.uilab.ui.weather.DailyPoint
import com.oopnv70.uilab.ui.weather.DynamicIslandCapsule
import com.oopnv70.uilab.ui.weather.HourlyPage
import com.oopnv70.uilab.ui.weather.HourlyPoint
import com.oopnv70.uilab.ui.weather.OverviewPage
import com.oopnv70.uilab.ui.weather.SavedCity
import com.oopnv70.uilab.ui.weather.SkyCondition
import com.oopnv70.uilab.ui.weather.WeatherGroup
import com.oopnv70.uilab.ui.weather.WeatherUiState
import com.oopnv70.uilab.ui.weather.WeatherViewModel
import com.oopnv70.uilab.ui.weather.cloudIcon
import com.oopnv70.uilab.ui.weather.mdiCog
import com.oopnv70.uilab.ui.weather.observedTimeText
import com.oopnv70.uilab.ui.weather.pressureIcon
import com.oopnv70.uilab.ui.weather.sunIcon
import com.oopnv70.uilab.ui.weather.sunriseIcon
import com.oopnv70.uilab.ui.weather.toCurrentWeather
import com.oopnv70.uilab.ui.weather.toDailyPoints
import com.oopnv70.uilab.ui.weather.toHourlyPoints
import com.oopnv70.uilab.ui.weather.toMetrics
import com.oopnv70.uilab.ui.weather.toSkyCondition

/**
 * 应用主框架（天气版）——**真实数据版**。
 *
 * 与之前版本的唯一区别：屏幕上所有天气数字都来自网络（Open-Meteo），
 * 城市列表来自**关键词网络搜索**，不再有任何手写假数据。
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
 * @param locationPermissionState 定位权限状态（由 MainActivity 申请后传入）。
 * @param permissionDiagnostics 权限诊断摘要（原始 FINE/COARSE 值 + 判定）。
 * @param locateState 定位结果状态（拿到真实城市名 + 坐标后去拉真实天气）。
 * @param onRetryLocate 手动重新定位的回调。
 * @param appSettings 当前应用设置（主题 / 单位等）。
 * @param onSettingsChange 用户在设置页改动设置后的回调（由调用方落盘）。
 */
// 液态玻璃用到的 RuntimeShader / AndroidView 桥接是 API 31+ 的。
// 调用处已经做了 `Build.VERSION.SDK_INT >= S` 的运行时守卫，
// 所以这里用 @SuppressLint("NewApi") 关掉 lint 的静态告警 ——
// 不这样做，lint 会因为「函数体内可能触达 31 的 API」而直接报错。
@SuppressLint("NewApi")
@Composable
fun LabApp(
    locationPermissionState: LocationPermissionState = LocationPermissionState.NOT_REQUESTED,
    permissionDiagnostics: String = "",
    locateState: LocateUiState = LocateUiState.Idle,
    onRetryLocate: () -> Unit = {},
    appSettings: AppSettingsState = AppSettingsState(),
    onSettingsChange: (AppSettingsState) -> Unit = {},
    weatherViewModel: WeatherViewModel = viewModel()
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    // 灵动岛是否展开（不跨进程保存，属于「临时 UI 状态」）
    var islandExpanded by remember { mutableStateOf(false) }
    // 设置页是否打开（同样是临时 UI 状态，不需要跨进程保存）
    var settingsOpen by rememberSaveable { mutableStateOf(false) }

    // 设置是覆盖层；系统返回键优先关闭它，不能直接结束 Activity。
    BackHandler(enabled = settingsOpen) {
        settingsOpen = false
    }

    val groups = WeatherGroup.entries

    // ---- 订阅 ViewModel 的真实数据 ----
    val savedCities by weatherViewModel.cities.collectAsState()
    val selectedCityName by weatherViewModel.selectedCityName.collectAsState()
    val weatherState by weatherViewModel.weather.collectAsState()
    val searchState by weatherViewModel.search.collectAsState()

    // 真实天气（就绪时才有）
    val realWeather = (weatherState as? WeatherUiState.Ready)?.weather
    val currentCityName = (weatherState as? WeatherUiState.Ready)?.cityName

    // ---- 定位成功 → 用真实坐标去拉真实天气（只做一次） ----
    LaunchedEffect(locateState) {
        val success = locateState as? LocateUiState.Success ?: return@LaunchedEffect
        val place = success.place
        // 注意：这里传的**不是** displayName。
        // displayName 现在会细分到「安徽省淮南市田家庵区洞山街道」，
        // 直接拿它当城市名会让卡片标题变得很长（「城市」栏位放不下）。
        // 城市名取最粗的两级：优先「市」，没有市则用「省」，
        // 都没有才退回「当前定位」。详细地址另外在定位卡片里展示。
        weatherViewModel.setLocatedCity(
            name = place.city ?: place.province ?: "当前定位",
            latitude = place.latitude,
            longitude = place.longitude
        )
    }

    // ---- 真实数据 → UI 展示模型 ----
    val currentWeather: CurrentWeather = remember(realWeather, currentCityName, weatherState) {
        val base = realWeather?.toCurrentWeather()
            ?: CurrentWeather(
                // 没有数据时不编造数字；摘要说明当前处在哪个阶段
                city = currentCityName.orEmpty(),
                condition = SkyCondition.CLOUDY,
                temperature = 0,
                feelsLike = 0,
                high = 0,
                low = 0,
                summary = when (weatherState) {
                    is WeatherUiState.Loading -> "正在获取真实天气…"
                    is WeatherUiState.Failed ->
                        "获取失败：${(weatherState as WeatherUiState.Failed).message}"
                    else -> "等待定位与城市选择…"
                }
            )
        base.copy(city = currentCityName ?: base.city)
    }

    val hourlyPoints: List<HourlyPoint> = remember(realWeather) {
        realWeather?.toHourlyPoints().orEmpty()
    }
    val dailyPoints: List<DailyPoint> = remember(realWeather) {
        realWeather?.toDailyPoints().orEmpty()
    }
    val metricItems = remember(realWeather) { realWeather?.toMetrics().orEmpty() }
    val updatedAt = remember(realWeather) { realWeather?.observedTimeText().orEmpty() }

    // ---- SavedCity → UI 的 CityItem ----
    val cityItems: List<CityItem> = remember(savedCities) {
        savedCities.map { it.toCityItem() }
    }

    // 系统栏避让
    val statusBarTop = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val navBottomInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
        .coerceAtLeast(16.dp)

    // ---- 下拉刷新状态 ----
    // 不能直接用 weatherState is Loading 当「刷新中」，
    // 否则首次进入页面（也在 Loading）会莫名其妙地转圈。
    // 所以这里单独记一个 refreshing：只有用户主动下拉才置 true，
    // 数据回来（不再是 Loading）再置回 false。
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(weatherState) {
        if (weatherState !is WeatherUiState.Loading) {
            refreshing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ---------- 内容区（支持下拉刷新） ----------
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                weatherViewModel.refresh()
            },
            modifier = Modifier.fillMaxSize()
        ) {
        // ---------- 内容区 ----------
        // 转场设计（解决「切换生硬」）：
        //   1. 方向感知：往右切（索引变大）新页从右侧滑入，往左切从左侧滑入。
        //      这样「概览 → 城市」和「城市 → 概览」的方向是相反的，
        //      符合空间直觉，不再是原地闪一下。
        //   2. 组合动画：位移 + 淡入淡出 + 轻微缩放（0.98 → 1.0），
        //      比纯淡入淡出更有「翻页」的实体感。
        //   3. 时长 300/220 ms：低于 250ms 人眼会觉得突兀，300ms 左右最自然。
        //   4. 关掉 SizeTransform：不同页面高度不同，默认的尺寸动画会把内容
        //      强行拉伸/裁剪，反而制造抖动。
        AnimatedContent(
            targetState = selectedIndex,
            transitionSpec = {
                val forward = targetState > initialState
                // 新页面入场方向：向右切 → 从右边进来；向左切 → 从左边进来
                val enterFrom = if (forward) 1 else -1
                // 旧页面退场方向：与入场相反
                val exitTo = -enterFrom

                val enter = slideInHorizontally(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    initialOffsetX = { full -> enterFrom * full / 6 }
                ) + fadeIn(
                    animationSpec = tween(220, delayMillis = 60)
                ) + scaleIn(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    initialScale = 0.97f
                )

                val exit = slideOutHorizontally(
                    animationSpec = tween(220, easing = FastOutLinearInEasing),
                    targetOffsetX = { full -> exitTo * full / 8 }
                ) + fadeOut(
                    animationSpec = tween(140)
                )

                // 注意：ContentTransform.sizeTransform 是只读的 val，
                // 不能用 apply { sizeTransform = null } 赋值（会编译失败）。
                // 官方文档明确支持：不需要尺寸动画时，在构造函数里传 sizeTransform = null。
                ContentTransform(
                    targetContentEnter = enter,
                    initialContentExit = exit,
                    targetContentZIndex = 0f,
                    sizeTransform = null
                )
            },
            label = "weatherTabContent"
        ) { index ->
            when (groups[index]) {
                WeatherGroup.OVERVIEW -> OverviewPage(
                    current = currentWeather,
                    metrics = metricItems,
                    updatedAt = updatedAt,
                    sunrise = realWeather?.sunrise.orEmpty(),
                    sunset = realWeather?.sunset.orEmpty()
                )

                WeatherGroup.HOURLY -> HourlyPage(hourly = hourlyPoints)

                WeatherGroup.DAILY -> DailyPage(daily = dailyPoints)

                WeatherGroup.CITIES -> CitiesPage(
                    locationPermissionState = locationPermissionState,
                    permissionDiagnostics = permissionDiagnostics,
                    locateState = locateState,
                    cities = cityItems,
                    selectedCity = selectedCityName,
                    onSelectCity = { city ->
                        savedCities.firstOrNull { it.name == city.name }?.let {
                            weatherViewModel.selectCity(it)
                        }
                    },
                    // 城市增删已由「搜索结果入口」接管，这里保留兼容签名
                    onAddCity = {},
                    onRemoveCity = { city -> weatherViewModel.removeCity(city.name) },
                    onRetryLocate = onRetryLocate,
                    searchState = searchState,
                    onSearchQueryChange = { weatherViewModel.onSearchQueryChanged(it) },
                    onPickSearchResult = { place ->
                        weatherViewModel.addCity(place)
                        weatherViewModel.clearSearch()
                    }
                )
            }
        }
        } // ← 闭合 PullToRefreshBox
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
                onSelect = { selectedIndex = it },
                // 玻璃模式下，胶囊导航栏本身由真·液态玻璃承托。
                //
                // 符合 Apple 的液态玻璃规范：玻璃只用于「导航与控件」
                //（工具栏 / Tab Bar / 浮动按钮），不覆盖内容区。
                // 胶囊导航栏属于导航控件 → 该玻璃化；下面的天气内容卡片则不该。
                liquidGlass = GlassFeature.enabled && appSettings.themeStyle == ThemeStyle.LIQUID_GLASS
            )
        }
        // ---------- 顶部：灵动岛胶囊（最后画 → 层级最高） ----------
        //
        // 【暂时屏蔽】2026-09：用户认为顶部这个胶囊实际用处不大、观感也一般，
        // 先隐藏。这里刻意**只注释掉绘制调用**，而不是删除：
        //   - DynamicIslandCapsule.kt 组件本体、参数、动画逻辑全部保留完好；
        //   - 状态 islandExpanded 也保留（否则重开时还要再改一处）；
        // 想恢复的话，把下面这段 Box 取消注释、并解开 import 即可，改动仅两处。
        //
        // 注意：currentWeather 仍被上方内容区使用，不要一起删掉。
        if (SHOW_DYNAMIC_ISLAND) {
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

        // ---------- 右上角：设置按钮 ----------
        //
        // 为什么放右上角浮动按钮，而不是在底部导航栏加第 5 个 Tab：
        //   底部胶囊导航栏是按 items.size 等分宽度的（见 FloatingPillNavigationBar），
        //   从 4 格变 5 格会让「概览 / 逐时 / 预报 / 城市」四个中文标签明显变窄，
        //   且「设置」与天气四个大类不同级（前者是功能入口，后者是内容分区），
        //   混在一起语义也不对。右上角齿轮是天气 App 的通行位置，且零布局回归风险。
        //
        // 只在设置页关闭时显示：设置页已全屏覆盖，不需要再叠一个按钮。
        if (!settingsOpen) {
            // 玻璃模式下，齿轮按钮由真·液态玻璃承托。
            //
            // ⚠️ 结构上必须让玻璃**后画**（在 Box 里后声明 = 画在上层），
            //    因为 GlassView 要抓的是"它身后"的内容 —— 也就是下面的天气页。
            //    所以这里不是"给按钮加个背景"，而是"在按钮位置叠一层玻璃"。
            val glassMode = GlassFeature.enabled &&
                appSettings.themeStyle == ThemeStyle.LIQUID_GLASS

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        end = 14.dp,
                        // 与灵动岛同高：灵动岛隐藏时，这里就是右上角最佳位置
                        top = statusBarTop + 6.dp
                    )
                    .size(42.dp)
                    .clip(CircleShape)
                    // 玻璃模式下不画实心底 —— 由 GlassView 负责那块视觉；
                    // 默认模式维持原来的半透明实心圆底。
                    .then(
                        if (glassMode) Modifier
                        else Modifier.background(
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
                        )
                    )
                    .clickable { settingsOpen = true },
                contentAlignment = Alignment.Center
            ) {
                // ---- 真·液态玻璃层 ----
                // 放在图标之前声明，这样它在下层、图标浮在上面。
                // （GlassView 自己会去抓背后的天气页内容，与这里的兄弟节点无关。）
                if (glassMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    LiquidGlassSurface(
                        modifier = Modifier.fillMaxSize(),
                        cornerRadiusDp = 999f,          // 圆形
                        refract = GlassDefaults.REFRACT,
                        tint = GlassDefaults.TINT,
                        backdropBlur = 5f
                    )
                }
                Icon(
                    // 同 buildNavItems：图标描边/填充色在构建时已固化，
                    // 这里给确定实色，真实颜色由 Icon(tint) 覆盖。
                    imageVector = mdiCog(Color.Black),
                    contentDescription = "打开设置",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // ---------- 设置页（全屏覆盖，层级最高） ----------
        //
        // 用 AnimatedVisibility 做上下滑入 + 淡入，表达「这是一个叠加的面板」。
        // 放在最后 → 绘制在最上层，能盖住底部导航栏与设置按钮。
        AnimatedVisibility(
            visible = settingsOpen,
            enter = slideInVertically(
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                initialOffsetY = { full -> full / 12 }
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                animationSpec = tween(220, easing = FastOutLinearInEasing),
                targetOffsetY = { full -> full / 12 }
            ) + fadeOut(animationSpec = tween(160)),
            label = "settingsPage"
        ) {
            SettingsPage(
                settings = appSettings,
                onSettingsChange = onSettingsChange,
                onClose = { settingsOpen = false },
                appVersion = APP_VERSION
            )
        }
    }
}

/**
 * 是否显示顶部灵动岛胶囊。
 *
 * 置为 false = 暂时隐藏（当前默认）。
 * 想恢复成 true 时，记得同时解开 `DynamicIslandCapsule` 的 import
 * （那个 import 在 false 分支下用不到，编译器会有未使用提示）。
 */
private const val SHOW_DYNAMIC_ISLAND = false

/**
 * 展示在设置页「关于」里的版本号。
 *
 * 为什么不直接读 BuildConfig.VERSION_NAME：
 *   ui-lab 的 versionName 是 `0.5-fixed-signing`（见 app/build.gradle.kts），
 *   那是给构建/签名追踪用的内部标识，带后缀，展示给用户看不够干净。
 *   这里单独维护一个「面向用户」的版本号。
 *   ⚠️ 发版时需与 build.gradle.kts 的 versionName 一起改，两者不要脱节。
 */
private const val APP_VERSION = "0.5"

/**
 * 把 ViewModel 的 [SavedCity] 转成 UI 层用的 [CityItem]。
 *
 * 温度未知时用 [TEMP_UNKNOWN] 哨兵值表示「暂无」——UI 的城市卡片会渲染成「—」。
 * （CityItem.temperature 是 Int 而非 Int?，这里用哨兵值而不是编造一个温度。）
 */
private fun SavedCity.toCityItem(): CityItem = CityItem(
    name = name,
    admin = subtitle,
    temperature = temperature ?: TEMP_UNKNOWN,
    condition = kind?.toSkyCondition() ?: SkyCondition.CLOUDY,
    isCurrent = isCurrent
)

/** 「温度未知」哨兵值。UI 见到它显示「—」。 */
const val TEMP_UNKNOWN: Int = Int.MIN_VALUE

/**
 * 构建导航项：四个天气大类，使用自绘图标。
 *
 * 关于 [Color.Black] 这个「看起来写死颜色」的写法：
 * 自绘图标是**描边**图形，描边色在构建 ImageVector 时就被 `SolidColor(tint)`
 * 固化进路径里了。如果这里传 `Color.Unspecified`，描边就会以「未指定颜色」
 * 绘制——结果是四个 Tab 看上去一片空白（图标其实在，但画不出来）。
 *
 * 传一个确定的实色（黑色）让描边有颜色，真正的显示颜色再由
 * `Icon(tint = ...)` 通过 ColorFilter 覆盖，从而跟随导航栏的选中 / 未选中状态。
 */
private fun buildNavItems(): List<NavItem> {
    val paint = Color.Black
    return listOf(
        NavItem(icon = sunIcon(paint), label = WeatherGroup.OVERVIEW.label),
        NavItem(icon = sunriseIcon(paint), label = WeatherGroup.HOURLY.label),
        NavItem(icon = cloudIcon(paint), label = WeatherGroup.DAILY.label),
        NavItem(icon = pressureIcon(paint), label = WeatherGroup.CITIES.label)
    )
}