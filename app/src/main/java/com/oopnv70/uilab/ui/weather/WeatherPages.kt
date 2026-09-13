package com.oopnv70.uilab.ui.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oopnv70.uilab.location.LocateStage
import com.oopnv70.uilab.location.LocateUiState
import com.oopnv70.uilab.location.LocationPermissionState
import com.oopnv70.uilab.location.cityDisplayName
import com.oopnv70.uilab.location.description
import com.oopnv70.uilab.location.rememberLocationRequester

// =====================================================================
// 四个天气大类页面
// =====================================================================
// 说明：内容全部来自 MockWeather（假数据），目的是先把「样子」做出来。
// 接真实数据时，只需把 MockWeather.xxx 换成 ViewModel 的数据源，
// 页面结构本身不用动。
//
// 页面顶部都预留了 TopPaddingForIsland 的空隙，
// 因为灵动岛胶囊是「悬浮」在内容之上的（不占布局空间）。
// =====================================================================

/** 灵动岛悬浮区的总高度（收起态 + 上下留白），页面顶部需避开。 */
val TopPaddingForIsland = 76.dp

/** 页面通用外壳：统一顶部避让 + 底部留白（避让导航栏）。 */
@Composable
private fun WeatherPageScaffold(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = TopPaddingForIsland)
    ) {
        content()
    }
}

/** 卡片容器（统一圆角 / 配色 / 内边距）。 */
@Composable
private fun WeatherCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}

/** 小节标题。 */
@Composable
private fun SectionTitle(text: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/** 来源标注的小字（呼应「小字标注来源」）。 */
@Composable
private fun SourceTag(source: WeatherSource) {
    Text(
        text = source.displayName,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        fontSize = 9.sp
    )
}

// =====================================================================
// 1. 概览页
// =====================================================================
@Composable
fun OverviewPage(
    modifier: Modifier = Modifier,
    current: CurrentWeather = MockWeather.current
) {

    WeatherPageScaffold {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ---------- 主卡片：大温度 ----------
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp,
                    shadowElevation = 3.dp
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = locationIcon(MaterialTheme.colorScheme.onPrimaryContainer),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = current.city,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = "更新 ${MockWeather.UPDATED_AT}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = current.condition.icon(
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    contentDescription = current.condition.label,
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text(
                                            text = "${current.temperature}",
                                            fontSize = 68.sp,
                                            fontWeight = FontWeight.Light,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "°",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Light,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = current.condition.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "${current.high}° / ${current.low}°",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = current.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // ---------- 关键指标网格 ----------
            item {
                WeatherCard {
                    Column {
                        SectionTitle("关键指标", trailing = "点击查看详情")
                        Spacer(Modifier.height(14.dp))

                        val metrics = MockWeather.metrics
                        // 两列排布
                        metrics.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { metric ->
                                    MetricTile(metric = metric, modifier = Modifier.weight(1f))
                                }
                                // 补齐空位，保证对齐
                                if (rowItems.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }

            // ---------- 日出日落 ----------
            item {
                WeatherCard {
                    Column {
                        SectionTitle("日出 · 日落")
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SunTimeBlock(
                                title = "日出",
                                time = MockWeather.sunrise.value,
                                source = MockWeather.sunrise.source,
                                modifier = Modifier.weight(1f)
                            )
                            SunTimeBlock(
                                title = "日落",
                                time = MockWeather.sunset.value,
                                source = MockWeather.sunset.source,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ---------- 数据来源说明 ----------
            item {
                WeatherCard {
                    Column {
                        SectionTitle("数据来源")
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "本应用融合多个公开气象数据源，每个数值均标注来源，" +
                                    "并统一到同一位置与同一观测时刻，避免混用造成偏差。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** 单个指标块。 */
@Composable
private fun MetricTile(metric: WeatherMetric, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = metric.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = metric.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = metric.value,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = metric.unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 3.dp, bottom = 4.dp)
                )
            }
            if (metric.note != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = metric.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            SourceTag(metric.source)
        }
    }
}

/** 日出 / 日落块。 */
@Composable
private fun SunTimeBlock(
    title: String,
    time: String,
    source: WeatherSource,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = sunriseIcon(MaterialTheme.colorScheme.onSurfaceVariant),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = time,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            SourceTag(source)
        }
    }
}

// =====================================================================
// 2. 逐时页
// =====================================================================
@Composable
fun HourlyPage(modifier: Modifier = Modifier) {
    val hourly = MockWeather.hourly

    WeatherPageScaffold {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                WeatherCard {
                    Column {
                        SectionTitle("未来 24 小时", trailing = "横向滑动查看")
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            // 横向逐时
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(hourly) { point ->
                        HourlyChip(point = point)
                    }
                }
            }

            // 温度走势（用条形近似折线）
            item {
                WeatherCard {
                    Column {
                        SectionTitle("温度走势", trailing = "℃")
                        Spacer(Modifier.height(14.dp))
                        TemperatureBars(points = hourly)
                        Spacer(Modifier.height(8.dp))
                        SourceTag(WeatherSource.OPEN_METEO)
                    }
                }
            }

            // 降水概率
            item {
                WeatherCard {
                    Column {
                        SectionTitle("降水概率", trailing = "%")
                        Spacer(Modifier.height(12.dp))
                        hourly.filter { it.precipitation > 0 }.forEach { point ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = point.time,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(52.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(point.precipitation / 100f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Text(
                                    text = "${point.precipitation}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .width(44.dp)
                                        .padding(start = 8.dp)
                                )
                            }
                        }
                        if (hourly.none { it.precipitation > 0 }) {
                            Text(
                                text = "未来 24 小时无降水",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 单个逐时小卡。 */
@Composable
private fun HourlyChip(point: HourlyPoint) {
    val highlight = point.isNow
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (highlight) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        tonalElevation = if (highlight) 2.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .width(66.dp)
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = point.time,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
                color = if (highlight) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.height(8.dp))
            Icon(
                imageVector = point.condition.icon(
                    if (highlight) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                ),
                contentDescription = point.condition.label,
                modifier = Modifier.size(22.dp),
                tint = if (highlight) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${point.temperature}°",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (highlight) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (point.precipitation > 0) "${point.precipitation}%" else "—",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * 温度走势条。
 * 用一排竖条近似折线图（不引入图表库，保持零依赖）。
 */
@Composable
private fun TemperatureBars(points: List<HourlyPoint>) {
    val min = points.minOf { it.temperature }
    val max = points.maxOf { it.temperature }
    val range = (max - min).coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEach { point ->
            val ratio = (point.temperature - min).toFloat() / range
            // 条高：最低 20% 最高 100%
            val heightFraction = 0.2f + ratio * 0.8f
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // 注意：fillMaxSize 没有 fraction 重载，这里必须用
                // fillMaxHeight(fraction) 才能「按比例长高」。
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction = heightFraction)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (point.isNow) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            }
                        )
                )
            }
        }
    }
}

// =====================================================================
// 3. 预报页（7 天）
// =====================================================================
@Composable
fun DailyPage(modifier: Modifier = Modifier) {
    val daily = MockWeather.daily

    WeatherPageScaffold {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                WeatherCard {
                    Column {
                        SectionTitle("未来 7 天", trailing = "共 ${daily.size} 天")
                        Spacer(Modifier.height(8.dp))
                        daily.forEachIndexed { index, day ->
                            DailyRow(day = day, isFirst = index == 0)
                            if (index != daily.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            item {
                WeatherCard {
                    Column {
                        SectionTitle("温度区间对比")
                        Spacer(Modifier.height(14.dp))
                        DailyRangeBars(days = daily)
                        Spacer(Modifier.height(10.dp))
                        SourceTag(WeatherSource.OPEN_METEO)
                    }
                }
            }
        }
    }
}

/** 一天的预报行。 */
@Composable
private fun DailyRow(day: DailyPoint, isFirst: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(64.dp)) {
            Text(
                text = day.weekday,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isFirst) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = day.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Icon(
            imageVector = day.condition.icon(MaterialTheme.colorScheme.onSurfaceVariant),
            contentDescription = day.condition.label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(10.dp))

        Text(
            text = day.condition.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp)
        )

        // 降水概率
        Text(
            text = if (day.precipitation > 0) "${day.precipitation}%" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(40.dp)
        )

        Spacer(Modifier.weight(1f))

        // 高低温
        Text(
            text = "${day.low}°",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = " / ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = "${day.high}°",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** 温度区间条形图（每天一条，显示最低~最高区间位置）。 */
@Composable
private fun DailyRangeBars(days: List<DailyPoint>) {
    val globalMin = days.minOf { it.low }
    val globalMax = days.maxOf { it.high }
    val range = (globalMax - globalMin).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        days.forEach { day ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = day.weekday,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(38.dp)
                )
                // 整条底槽
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    val startFraction = (day.low - globalMin).toFloat() / range
                    val endFraction = (day.high - globalMin).toFloat() / range
                    val spanFraction = (endFraction - startFraction).coerceAtLeast(0.08f)

                    // 区间条：先用 Spacer 把左侧「推」出去，再放实际区间
                    // （用 offset 会比 Spacer 更精确，但 offset 需要 Dp，
                    //   这里用「占位 + 比例块」的组合，纯比例实现，无需测量）
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (startFraction > 0f) {
                            Spacer(modifier = Modifier.fillMaxWidth(startFraction))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(spanFraction / (1f - startFraction).coerceAtLeast(0.01f))
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
                        )
                    }
                }
                Text(
                    text = "${day.low}~${day.high}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .width(64.dp)
                        .padding(start = 8.dp)
                )
            }
        }
    }
}

// =====================================================================
// 4. 城市页
// =====================================================================
@Composable
fun CitiesPage(
    locationPermissionState: LocationPermissionState = LocationPermissionState.NOT_REQUESTED,
    locateState: LocateUiState = LocateUiState.Idle,
    onRetryLocate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 城市列表：把「当前位置」换成真实定位城市
    val cities = remember(locateState) {
        MockWeather.citiesWith(locateState.cityDisplayName)
    }
    // 手动再次申请定位权限（例如用户第一次点了「不允许」）
    val requestLocation = rememberLocationRequester()

    WeatherPageScaffold {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------- 定位卡片（权限 + 定位结果） ----------
            item {
                LocationPermissionCard(
                    state = locationPermissionState,
                    locateState = locateState,
                    onRequest = { requestLocation() },
                    onRetry = onRetryLocate
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已添加城市",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${cities.size} 个",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            itemsIndexed(cities) { _, city ->
                CityCard(city = city)
            }

            item {
                WeatherCard {
                    Column {
                        SectionTitle("关于数据来源")
                        Spacer(Modifier.height(10.dp))
                        WeatherSource.entries.forEach { source ->
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = source.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = source.shortName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 城市卡片。 */
@Composable
private fun CityCard(city: CityItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (city.isCurrent) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = city.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (city.isCurrent) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "当前",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = city.admin,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = city.condition.icon(MaterialTheme.colorScheme.onSurfaceVariant),
                contentDescription = city.condition.label,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${city.temperature}°",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
// =====================================================================
// 定位卡片（城市页顶部）：权限状态 + 定位结果
// =====================================================================
/**
 * 显示当前定位权限状态 + 实际定位结果，并给出可操作按钮。
 *
 * 权限状态 → 呈现：
 *   NOT_REQUESTED        → 灰点 + "使用当前位置" + "授权"按钮
 *   GRANTED_PRECISE      → 绿点 + "已获得精确位置"
 *   GRANTED_APPROXIMATE  → 黄点 + "仅有大致位置" + "提精确"按钮
 *   DENIED               → 红点 + "定位权限被拒绝" + "重试"按钮
 *   DENIED_PERMANENTLY   → 红点 + "去设置"按钮
 *
 * 定位结果（locateState）→ 在第二行显示：
 *   Locating  → "正在定位…"
 *   Success   → 真实城市名，如"广东省深圳市"
 *   Failed    → 失败原因 + "重试定位"按钮
 */
@Composable
private fun LocationPermissionCard(
    state: LocationPermissionState,
    locateState: LocateUiState,
    onRequest: () -> Unit,
    onRetry: () -> Unit
) {
    // ---------- 权限状态的颜色与文案 ----------
    val dotColor = when (state) {
        LocationPermissionState.GRANTED_PRECISE -> Color(0xFF2E7D32)   // 绿
        LocationPermissionState.GRANTED_APPROXIMATE -> Color(0xFFF9A825) // 黄
        LocationPermissionState.DENIED,
        LocationPermissionState.DENIED_PERMANENTLY -> Color(0xFFC62828)  // 红
        LocationPermissionState.NOT_REQUESTED -> MaterialTheme.colorScheme.outline
    }
    val title = when (state) {
        LocationPermissionState.NOT_REQUESTED -> "使用当前位置"
        LocationPermissionState.GRANTED_PRECISE -> "已获得精确定位权限"
        LocationPermissionState.GRANTED_APPROXIMATE -> "仅获得大致位置权限"
        LocationPermissionState.DENIED -> "定位权限被拒绝"
        LocationPermissionState.DENIED_PERMANENTLY -> "定位权限被永久拒绝"
    }

    // ---------- 定位结果的副标题 ----------
    val subtitle = when (locateState) {
        is LocateUiState.Locating -> when (locateState.stage) {
            LocateStage.LOCATING -> "正在获取坐标…"
            LocateStage.REVERSE_GEOCODING -> "正在解析位置…"
            else -> "正在定位…"
        }
        is LocateUiState.Success -> "定位到：${locateState.place.displayName}"
        is LocateUiState.Failed -> locateState.reason
        LocateUiState.Idle -> state.description()
    }

    // 定位是否进行中（决定要不要显示转圈）
    val busy = locateState is LocateUiState.Locating

    WeatherCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = locationIcon(MaterialTheme.colorScheme.primary),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(dotColor)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (locateState is LocateUiState.Failed) {
                            Color(0xFFC62828)
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                }

                // ---------- 右侧按钮 ----------
                Spacer(Modifier.width(10.dp))
                when {
                    // 正在定位：显示"定位中"（不可点）
                    busy -> {
                        IdleLabel(text = "定位中…")
                    }
                    // 定位失败但权限有了：给"重试定位"
                    locateState is LocateUiState.Failed && state.isGranted -> {
                        ActionChip(text = "重试定位", onClick = onRetry)
                    }
                    // 权限未授予：给"授权/提精确/重试/去设置"
                    state != LocationPermissionState.GRANTED_PRECISE -> {
                        val buttonText = when (state) {
                            LocationPermissionState.NOT_REQUESTED -> "授权"
                            LocationPermissionState.GRANTED_APPROXIMATE -> "提精确"
                            LocationPermissionState.DENIED -> "重试"
                            LocationPermissionState.DENIED_PERMANENTLY -> "去设置"
                            LocationPermissionState.GRANTED_PRECISE -> ""
                        }
                        ActionChip(text = buttonText, onClick = onRequest)
                    }
                    // 权限有了、定位也有了：什么都不用点
                    else -> {
                        if (locateState is LocateUiState.Success) {
                            IdleLabel(text = "已定位")
                        }
                    }
                }
            }
        }
    }
}

/** 可点击的小胶囊按钮。 */
@Composable
private fun ActionChip(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
        )
    }
}

/** 不可点击的状态标签。 */
@Composable
private fun IdleLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
    )
}
