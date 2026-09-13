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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oopnv70.uilab.data.GeoPlace
import com.oopnv70.uilab.ui.TEMP_UNKNOWN
import com.oopnv70.uilab.location.LocateStage
import com.oopnv70.uilab.location.LocateUiState
import com.oopnv70.uilab.location.LocationPermissionState
import com.oopnv70.uilab.location.LocationPermissions
import com.oopnv70.uilab.location.PrecisionLevel
import com.oopnv70.uilab.location.cityDisplayName
import com.oopnv70.uilab.location.description
import com.oopnv70.uilab.location.isPermanentlyDenied
import com.oopnv70.uilab.location.openAppSettings
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
    current: CurrentWeather,
    /** 关键指标（来自真实数据）。 */
    metrics: List<WeatherMetric> = emptyList(),
    /** 更新时间文案（来自接口观测时刻）。 */
    updatedAt: String = "",
    /** 日出时间（真实）。 */
    sunrise: String = "",
    /** 日落时间（真实）。 */
    sunset: String = ""
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
                                    text = if (updatedAt.isBlank()) "实时" else "更新 $updatedAt",
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

                        // 真实指标：全部来自 Open-Meteo，无数据时显示「—」
                        if (metrics.isEmpty()) {
                            Text(
                                text = "正在获取真实数据…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
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
                                time = sunrise.ifBlank { "—" },
                                source = WeatherSource.OPEN_METEO,
                                modifier = Modifier.weight(1f)
                            )
                            SunTimeBlock(
                                title = "日落",
                                time = sunset.ifBlank { "—" },
                                source = WeatherSource.OPEN_METEO,
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
fun HourlyPage(
    modifier: Modifier = Modifier,
    /** 真实逐时数据（来自 Open-Meteo）。空列表表示还在加载。 */
    hourly: List<HourlyPoint> = emptyList()
) {
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

            // 降水概率：突出「下个小时」，下面配 24 小时概率柱状图形表
            item {
                WeatherCard {
                    Column {
                        val nextHour = hourly.getOrNull(1)
                        val nextProb = nextHour?.precipitation
                        val nextLabel = nextHour?.time ?: "—"

                        SectionTitle("降水概率", trailing = nextLabel)

                        Spacer(Modifier.height(10.dp))

                        // ---- 大字号突出「下个小时」 ----
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "下个小时降雨概率",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = nextProb?.let { "$it%" } ?: "—",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        nextProb == null -> MaterialTheme.colorScheme.outline
                                        nextProb >= 60 -> MaterialTheme.colorScheme.primary
                                        nextProb > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Text(
                                text = precipitationHint(nextProb),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        )
                        Spacer(Modifier.height(12.dp))

                        // ---- 24 小时概率柱状图形表（横向可滑动，柱子够粗才看得清） ----
                        // 先说明这排柱子是什么：光有数字没有单位，谁也看不懂。
                        Text(
                            text = "未来 24 小时 · 每小时降水概率（%）",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        PrecipitationChart(points = hourly, highlightIndex = 1)

                        Spacer(Modifier.height(10.dp))
                        SourceTag(WeatherSource.OPEN_METEO)
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
 * 温度走向图。
 * 之前用「整行均分 + 极窄柱子」画，24 个点挤在一行里每根只有几像素宽，
 * 看起来只剩一排细缝、像没画出来。改成横向可滑动的粗柱图 + 顶端温度值，
 * 每根柱子固定 30dp 宽、88dp 高，走势一眼可见。
 */
@Composable
private fun TemperatureBars(points: List<HourlyPoint>) {
    // 空列表保护：加载中 / 请求失败时不要崩（minOf 会在空集合上抛异常）
    if (points.isEmpty()) {
        Text(
            text = "暂无逐时数据",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        return
    }
    val min = points.minOf { it.temperature }
    val max = points.maxOf { it.temperature }
    val range = (max - min).coerceAtLeast(1)
    val primary = MaterialTheme.colorScheme.primary

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(points) { point ->
            val ratio = (point.temperature - min).toFloat() / range
            // 柱高：最低 15% 最高 100%，保证低温日也看得见
            val heightFraction = 0.15f + ratio * 0.85f
            Column(
                modifier = Modifier.width(34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 柱子（从底部往上长）
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(88.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .fillMaxHeight(fraction = heightFraction)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 7.dp,
                                    topEnd = 7.dp,
                                    bottomEnd = 0.dp,
                                    bottomStart = 0.dp
                                )
                            )
                            .background(
                                if (point.isNow) primary
                                else primary.copy(alpha = 0.4f)
                            )
                    )
                }
                Spacer(Modifier.height(4.dp))
                // 温度值
                Text(
                    text = "${point.temperature}°",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (point.isNow) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (point.isNow) primary else MaterialTheme.colorScheme.onSurface
                )
                // 时间
                Text(
                    text = point.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 降水概率图形表。
 * 以柱状图呈现未来 24 小时的降水概率，并把「下一个小时」高亮（[highlightIndex]），
 * 概率越高柱子越高、颜色越实。
 */
@Composable
private fun PrecipitationChart(
    points: List<HourlyPoint>,
    highlightIndex: Int = 1
) {
    if (points.isEmpty()) {
        Text(
            text = "暂无逐时数据",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        return
    }
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(points) { index, point ->
            val isNext = index == highlightIndex
            val prob = point.precipitation.coerceIn(0, 100)
            // 柱高：最低 6%（0% 也留一条底线），最高 100%
            val heightFraction = 0.06f + (prob / 100f) * 0.94f
            Column(
                modifier = Modifier.width(34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 概率数值（带 % 单位，否则一排数字看不出含义）
                Text(
                    text = if (prob > 0) "$prob%" else "0%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isNext) primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                // 轨道 + 柱体
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(track.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .fillMaxHeight(fraction = heightFraction)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                when {
                                    isNext -> primary
                                    prob >= 60 -> primary.copy(alpha = 0.75f)
                                    prob > 0 -> primary.copy(alpha = 0.45f)
                                    else -> track
                                }
                            )
                    )
                }
                Spacer(Modifier.height(4.dp))
                // 时间（高亮的那个加粗）
                Text(
                    text = point.time,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isNext) primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 根据降水概率给一句人话说明。null 表示数据缺失。 */
private fun precipitationHint(prob: Int?): String = when {
    prob == null -> "暂无数据"
    prob == 0 -> "基本不会下雨"
    prob < 30 -> "下雨可能性较小"
    prob < 60 -> "可能有雨，可以留意"
    prob < 80 -> "下雨可能性较大"
    else -> "很可能下雨，记得带伞"
}

// =====================================================================
// 3. 预报页（7 天）
// =====================================================================
@Composable
fun DailyPage(
    modifier: Modifier = Modifier,
    /** 真实 7 天预报（来自 Open-Meteo）。空列表表示还在加载。 */
    daily: List<DailyPoint> = emptyList()
) {
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
    // 空列表保护：加载中 / 请求失败时不要崩（minOf 会在空集合上抛异常）
    if (days.isEmpty()) {
        Text(
            text = "暂无预报数据",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        return
    }
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
/**
 * 城市页：定位 + 城市切换 + 添加/移除城市。
 *
 * @param locationPermissionState 定位权限状态。
 * @param permissionDiagnostics 权限诊断摘要（原始值 + 判定），显示在定位卡片底部。
 * @param locateState 定位结果。
 * @param selectedCity 当前选中的城市名（受控，提升到 LabApp）。
 * @param onSelectCity 切换到某个城市。
 * @param onAddCity 添加一个城市（传入城市名）。
 * @param onRemoveCity 移除一个城市。
 * @param onRetryLocate 重新定位。
 */
@Composable
fun CitiesPage(
    locationPermissionState: LocationPermissionState = LocationPermissionState.NOT_REQUESTED,
    permissionDiagnostics: String = "",
    locateState: LocateUiState = LocateUiState.Idle,
    cities: List<CityItem> = emptyList(),
    selectedCity: String? = null,
    onSelectCity: (CityItem) -> Unit = {},
    onAddCity: (CityItem) -> Unit = {},
    onRemoveCity: (CityItem) -> Unit = {},
    onRetryLocate: () -> Unit = {},
    /** 网络搜索状态（真实搜索结果）。 */
    searchState: CitySearchState = CitySearchState.Idle,
    /** 输入关键词变化 → 触发网络搜索（内部有防抖）。 */
    onSearchQueryChange: (String) -> Unit = {},
    /** 点击某个搜索结果 → 真正加入城市列表（会去拉它的真实天气）。 */
    onPickSearchResult: (GeoPlace) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 手动再次申请定位权限（例如用户第一次点了「不允许」）
    val requestLocation = rememberLocationRequester()
    // 永久拒绝时只能去系统设置页手动开（requestPermissions 已不弹框）
    val context = LocalContext.current
    // 实时判断：系统层面的「拒绝且不再询问」。
    // 状态枚举里的 DENIED_PERMANENTLY 只有在 api 层能拿到 rationale 时才准，
    // 这里再用 shouldShowRequestPermissionRationale 兜一道底。
    val permanentlyDenied = remember(locationPermissionState) {
        !locationPermissionState.isGranted && isPermanentlyDenied(context, LocationPermissions.FINE)
    }

    // ---- 搜索/添加 面板开关 ----
    var showAddSheet by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    // 「可添加的城市」不再来自本地预置表，而是**实时网络搜索结果**。
    // 已加入列表的城市会被过滤掉，避免重复添加。
    val addable: List<GeoPlace> = remember(searchState, cities) {
        when (searchState) {
            is CitySearchState.Done ->
                searchState.results.filter { hit -> cities.none { it.name == hit.name } }
            else -> emptyList()
        }
    }

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
                    permanentlyDenied = permanentlyDenied,
                    diagnostics = permissionDiagnostics,
                    // 分派：永久拒绝 → 去设置页；否则 → 正常弹权限框。
                    // 以前这里两者都调 requestLocation()，导致「去设置」永远打不开。
                    onRequest = {
                        if (permanentlyDenied ||
                            locationPermissionState == LocationPermissionState.DENIED_PERMANENTLY
                        ) {
                            openAppSettings(context)
                        } else {
                            requestLocation()
                        }
                    },
                    onRetry = onRetryLocate
                )
            }

            // ---------- 标题行 + "添加城市"按钮 ----------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "我的城市",
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
                    Spacer(Modifier.width(10.dp))
                    // 添加城市
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showAddSheet = !showAddSheet }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = plusIcon(MaterialTheme.colorScheme.onPrimary),
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "添加",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            // ---------- 添加城市面板（可折叠） ----------
            if (showAddSheet) {
                item {
                    WeatherCard {
                        Column {
                            SectionTitle("添加城市")
                            Spacer(Modifier.height(10.dp))
                            // 搜索框
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = searchIcon(MaterialTheme.colorScheme.outline),
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    BasicTextField(
                                        value = query,
                                        onValueChange = {
                                            query = it
                                            // 每次输入都通知 ViewModel（内部 350ms 防抖后发请求）
                                            onSearchQueryChange(it)
                                        },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.weight(1f),
                                        decorationBox = { inner ->
                                            if (query.isEmpty()) {
                                                Text(
                                                    text = "搜索城市名，如「深圳」",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                            inner()
                                        }
                                    )
                                    if (query.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable { query = "" },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = closeIcon(MaterialTheme.colorScheme.outline),
                                                contentDescription = "清空",
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))

                            when {
                                // 输入为空 → 提示
                                query.isBlank() -> Text(
                                    text = "输入关键词搜索城市（支持中文/英文），有网就能搜到",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                // 正在请求网络
                                searchState is CitySearchState.Loading -> Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "正在搜索「$query」…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                // 请求失败
                                searchState is CitySearchState.Failed -> Text(
                                    text = "搜索失败：${searchState.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                // 搜到但没有结果
                                addable.isEmpty() -> Text(
                                    text = "没有找到「$query」，换个关键词试试（也可试试英文名）",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                // 真实搜索结果
                                else -> addable.forEach { candidate ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                // 真正加入：会去拉这个坐标的真实天气
                                                onPickSearchResult(candidate)
                                                query = ""
                                                showAddSheet = false
                                            }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = plusIcon(MaterialTheme.colorScheme.primary),
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = candidate.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (candidate.subtitle.isNotBlank()) {
                                                Text(
                                                    text = candidate.subtitle,
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
            }

            // ---------- 城市卡片列表 ----------
            itemsIndexed(cities) { _, city ->
                val isSelected = if (selectedCity == null) {
                    city.isCurrent
                } else {
                    city.name == selectedCity
                }
                CityCard(
                    city = city,
                    selected = isSelected,
                    onSelect = { onSelectCity(city) },
                    // 定位城市不允许移除（去掉它就没有"我在这"了）
                    onDelete = if (city.isCurrent) null else {
                        { onRemoveCity(city) }
                    }
                )
            }

            // ---------- 数据来源 ----------
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

/**
 * 城市卡片。
 *
 * @param city 城市数据。
 * @param selected 是否是「当前选中」的城市（点击后会高亮 + 显示对勾）。
 * @param onSelect 点击卡片 → 切换到该城市。
 * @param onDelete 点击右侧 × → 从列表移除（定位城市不可删）。
 */
@Composable
private fun CityCard(
    city: CityItem,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(20.dp),
        color = when {
            selected -> MaterialTheme.colorScheme.primaryContainer
            city.isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        // 选中态额外加一圈描边，避免只靠颜色区分（无障碍友好）
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null,
        tonalElevation = 1.dp,
        shadowElevation = if (selected) 4.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = city.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (city.isCurrent) {
                        Spacer(Modifier.width(8.dp))
                        Badge(text = "定位", filled = false)
                    }
                    if (selected) {
                        Spacer(Modifier.width(8.dp))
                        Badge(text = "当前", filled = true)
                    }
                }
                if (city.admin.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = city.admin,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = city.condition.icon(MaterialTheme.colorScheme.onSurfaceVariant),
                contentDescription = city.condition.label,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Text(
                // 温度未取到时不编造数字，显示「—」
                text = if (city.temperature == TEMP_UNKNOWN) "—" else "${city.temperature}°",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 删除按钮：定位城市不给删
            if (onDelete != null) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = closeIcon(MaterialTheme.colorScheme.outline),
                        contentDescription = "移除",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/** 小徽标（"定位" / "当前"）。 */
@Composable
private fun Badge(text: String, filled: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (filled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (filled) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
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
 *
 * @param diagnostics 权限诊断摘要（原始 FINE/COARSE 值 + 判定 + API 级别）。
 *        非空时在卡片底部显示一行小字，供截图取证。
 */
@Composable
private fun LocationPermissionCard(
    state: LocationPermissionState,
    locateState: LocateUiState,
    onRequest: () -> Unit,
    onRetry: () -> Unit,
    permanentlyDenied: Boolean = false,
    diagnostics: String = ""
) {
    // ---------- 权限状态的颜色与文案 ----------
    val dotColor = when (state) {
        LocationPermissionState.GRANTED_PRECISE -> Color(0xFF2E7D32)   // 绿
        LocationPermissionState.GRANTED_APPROXIMATE -> Color(0xFFF9A825) // 黄
        LocationPermissionState.DENIED,
        LocationPermissionState.DENIED_PERMANENTLY -> Color(0xFFC62828)  // 红
        LocationPermissionState.NOT_REQUESTED -> MaterialTheme.colorScheme.outline
    }
    val title = when {
        // 实时判断优先：系统层面"拒绝且不再询问"时，明确告诉用户要去设置
        permanentlyDenied && !state.isGranted -> "定位权限被永久拒绝"
        state == LocationPermissionState.NOT_REQUESTED -> "使用当前位置"
        state == LocationPermissionState.GRANTED_PRECISE -> "已获得精确定位权限"
        state == LocationPermissionState.GRANTED_APPROXIMATE -> "仅获得大致位置权限"
        state == LocationPermissionState.DENIED -> "定位权限被拒绝"
        else -> "定位权限被永久拒绝"
    }

    // ---------- 定位结果的副标题 ----------
    val subtitle = when (locateState) {
        is LocateUiState.Locating -> when (locateState.stage) {
            LocateStage.LOCATING -> "正在获取坐标…"
            LocateStage.REVERSE_GEOCODING -> "正在解析位置…"
            else -> "正在定位…"
        }
        // 定位成功：显示从省到街道的完整地址 + 括注精度等级。
        // 括注精度的原因是：不同机型能拿到的层级差别很大，
        // 明确告诉用户"只到市区"比含糊地显示一个城市名更诚实。
        is LocateUiState.Success -> {
            val p = locateState.place
            val level = p.precisionLevel
            // 未知等级时不显示括注，避免出现「（未知）」这种没信息量的尾巴
            if (level == PrecisionLevel.UNKNOWN) {
                "定位到：${p.displayName}"
            } else {
                "定位到：${p.displayName}（精度：${level.label}）"
            }
        }
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
                        val buttonText = when {
                            // 永久拒绝 → 去设置页（点下去真的会跳设置）
                            permanentlyDenied -> "去设置"
                            state == LocationPermissionState.NOT_REQUESTED -> "授权"
                            state == LocationPermissionState.GRANTED_APPROXIMATE -> "提精确"
                            state == LocationPermissionState.DENIED -> "重试"
                            state == LocationPermissionState.DENIED_PERMANENTLY -> "去设置"
                            else -> ""
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

            // ---------- 诊断行（截图取证用） ----------
            // 这一行只在「权限显示已授予、但定位拿不到」这种诡异组合下出现：
            // 它能直接告诉我们是系统原始值就是未授予，还是原始值已授予却没生效。
            if (diagnostics.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = diagnostics,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth()
                )
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
