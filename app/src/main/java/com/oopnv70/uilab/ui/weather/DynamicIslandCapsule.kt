package com.oopnv70.uilab.ui.weather

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =====================================================================
// 顶部灵动岛胶囊（Dynamic Island Capsule）
// =====================================================================
// 设计意图：
//   收起时 —— 一个【小小的药丸】，只显示「城市 + 温度」，静静待在顶部，
//             不抢视线。宽度自适应内容，不是横贯整屏。
//   点击后 —— 向下展开成一张完整的天气卡片，再点收起。
//
// 尺寸对照：
//   收起态：高 34dp，宽「按内容」，圆角 = 高/2（完全胶囊）
//   展开态：宽撑满可用空间，圆角 26dp
//
// ⚠️ 之前的问题：收起态就占了整行宽度 + 46dp 高，看着很碍眼。
//    本版把收起态做成「内容宽度 + 34dp 高」的小药丸。
// =====================================================================

/** 收起态高度（小药丸）。 */
private val CollapsedHeight = 34.dp

/**
 * 灵动岛展开 / 收起的统一过渡时长（毫秒）。
 * 宽度、高度、圆角、内部内容共用它，四条变化同起同落，
 * 才不会有「宽度已经到位、高度还在追」的撕裂感。
 */
private const val IslandTransitionMillis = 300

@Composable
fun DynamicIslandCapsule(
    current: CurrentWeather,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 箭头旋转：收起 0°，展开 180°
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "islandArrow"
    )

    // ------------------------------------------------------------------
    // 展开/收起的过渡动画
    //
    // 之前的问题：内容区（AnimatedVisibility）有动画，但「胶囊外框」是
    // 瞬间跳变的 —— 宽度、圆角、内边距都由 if (expanded) 直接二选一，
    // 于是展开的那一帧外框突然变宽变方，看起来就是「一瞬间弹出」。
    //
    // 现在把外框本身也纳入动画：宽度 / 圆角 / 内边距都用动画值驱动，
    // 外框与内容同步平滑变化。
    // ------------------------------------------------------------------
    // 展开/收起的过渡动画
    //
    // 之前的坑：手工用 animateDpAsState 插值「宽度」，但收起宽度必须是
    // 固定值才可插值 —— 结果内容（城市 + 温度 + 箭头）宽度和这个固定值
    // 对不上：城市名短则右边空一截，温度看着「位置不对」；而且宽度、
    // 高度、圆角三条动画各自为政，展开时就有撕裂感。
    //
    // 现在换成官方的 animateContentSize()：
    //   - 收起态用 wrapContentWidth()，尺寸完全由内容决定 → 温度永远
    //     紧跟城市名，位置自然。
    //   - 展开态切到 fillMaxWidth()，animateContentSize 负责把这次
    //     尺寸变化（宽 + 高）平滑补间，不再撕裂。
    //   - 圆角用 animateDpAsState 与尺寸同步过渡。
    //   - 展开时锚点固定在左上角（TopStart）：宽度向右推、高度向下长，
    //     左边缘和顶边缘不动，视觉上就是「从左开始缩放」。
    // ------------------------------------------------------------------

    // 所有过渡共用一个时长与缓动，宽 / 高 / 圆角才会同进同出，
    // 不会出现「宽度先到、高度后到」的撕裂感。
    val transitionSpec = tween<IntSize>(
        durationMillis = IslandTransitionMillis,
        easing = FastOutSlowInEasing
    )

    val corner by animateDpAsState(
        targetValue = if (expanded) 26.dp else CollapsedHeight / 2,
        animationSpec = tween(IslandTransitionMillis, easing = FastOutSlowInEasing),
        label = "islandCorner"
    )

    // 背景渐变：收起时更淡，展开时更饱满
    val gradient = Brush.linearGradient(
        colors = if (expanded) {
            listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.surfaceContainerHigh
            )
        } else {
            listOf(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }
    )

    Surface(
        modifier = modifier
            // 宽度：收起 = 按内容；展开 = 撑满。
            // 尺寸变化交给 animateContentSize 平滑处理（宽高一起补间）。
            .then(if (expanded) Modifier.fillMaxWidth() else Modifier.wrapContentWidth())
            // 显式指定 TopStart 对齐：宽高变化都以左上角为锚点，
            // 左边缘与顶边缘保持不动，看起来就是「从左边开始向右下缩放」。
            // 宽和高共用同一个 animationSpec，速度完全一致。
            .animateContentSize(
                animationSpec = transitionSpec,
                alignment = Alignment.TopStart
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            ),
        // 圆角也用动画值，收起是完整胶囊、展开平滑过渡到圆角矩形
        shape = RoundedCornerShape(corner),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 5.dp
    ) {
        Box(modifier = Modifier.background(gradient)) {
            Column {
                // ---------------- 收起态常驻的一行（小药丸） ----------------
                Row(
                    modifier = Modifier
                        .height(CollapsedHeight)
                        .padding(start = 12.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 天气图标（小）
                    Icon(
                        imageVector = current.condition.icon(
                            MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentDescription = current.condition.label,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(5.dp))

                    // 城市
                    Text(
                        text = current.city,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 展开态才显示天气文字，收起态尽量小
                    if (expanded) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = current.condition.label,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.width(5.dp))

                    // 温度
                    Text(
                        text = "${current.temperature}°",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 箭头（收起态也放，提示「可展开」）
                    Spacer(Modifier.width(3.dp))
                    ChevronIcon(
                        rotation = arrowRotation,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 13.dp
                    )
                }

                // ---------------- 展开态内容 ----------------
                // 高度动画与外框共用同一时长，内容长出来和外框变高的节奏一致。
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(
                        animationSpec = tween(IslandTransitionMillis, easing = FastOutSlowInEasing),
                        expandFrom = Alignment.Top
                    ) + fadeIn(tween(180, delayMillis = 60)),
                    exit = shrinkVertically(
                        animationSpec = tween(IslandTransitionMillis, easing = FastOutSlowInEasing),
                        shrinkTowards = Alignment.Top
                    ) + fadeOut(tween(120))
                ) {
                    ExpandedContent(current = current)
                }
            }
        }
    }
}

/** 展开后的详细内容。 */
@Composable
private fun ExpandedContent(current: CurrentWeather) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, bottom = 18.dp)
    ) {
        // 细分隔线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // 大图标
            Icon(
                imageVector = current.condition.icon(MaterialTheme.colorScheme.onSurface),
                contentDescription = current.condition.label,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(16.dp))

            Column {
                // 大温度
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "${current.temperature}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "°C",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Text(
                    text = current.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 高温 / 低温 / 体感
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniStat(
                label = "最高",
                value = "${current.high}°",
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                label = "最低",
                value = "${current.low}°",
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                label = "体感",
                value = "${current.feelsLike}°",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        // 数据来源标注（小字）
        Text(
            text = "数据来自 ${WeatherSource.OPEN_METEO.displayName}（实时）",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

/** 小统计块。 */
@Composable
private fun MiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 自绘的「展开箭头」图标。
 * 画一个向下的小折角，用 [rotate] 控制方向。
 */
@Composable
private fun ChevronIcon(
    rotation: Float,
    tint: Color,
    size: androidx.compose.ui.unit.Dp = 16.dp
) {
    val icon = remember(tint) {
        ImageVector.Builder(
            name = "chevron_down",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(tint),
                strokeLineWidth = 2.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 9.5f)
                lineTo(12f, 15.5f)
                lineTo(18f, 9.5f)
            }
        }.build()
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .rotate(rotation),
        tint = tint
    )
}