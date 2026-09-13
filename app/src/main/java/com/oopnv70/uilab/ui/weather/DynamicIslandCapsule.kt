package com.oopnv70.uilab.ui.weather

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =====================================================================
// 顶部灵动岛胶囊（Dynamic Island Capsule）
// =====================================================================
// 设计意图（来自用户）：
//   屏幕顶部悬浮一个「灵动岛」式胶囊，
//   收起时只显示一行摘要（城市 + 温度 + 天气图标），
//   点击后向下展开成一张完整的「当前天气卡片」。
//
// 视觉要点：
//   - 完全胶囊圆角（收起态），展开后依然保持大圆角
//   - 渐变背景，营造「玻璃质感」而非死板色块
//   - 展开 / 收起用垂直展开动画，带淡入淡出
//   - 右侧箭头随展开状态旋转 180°
// =====================================================================

/** 收起态高度。 */
private val CollapsedHeight = 46.dp

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
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "islandArrow"
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
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            ),
        shape = if (expanded) {
            RoundedCornerShape(28.dp)
        } else {
            RoundedCornerShape(CollapsedHeight / 2)
        },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        Box(modifier = Modifier.background(gradient)) {
            Column {
                // ---------------- 收起态常驻的一行 ----------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CollapsedHeight)
                        .padding(start = 16.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 天气图标（小）
                    Icon(
                        imageVector = current.condition.icon(
                            MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentDescription = current.condition.label,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))

                    // 城市
                    Text(
                        text = current.city,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = current.condition.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.weight(1f))

                    // 温度
                    Text(
                        text = "${current.temperature}°",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))

                    // 展开箭头（自绘，避免依赖图标库）
                    ChevronIcon(
                        rotation = arrowRotation,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ---------------- 展开态内容 ----------------
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(
                        animationSpec = tween(320, easing = FastOutSlowInEasing),
                        expandFrom = Alignment.Top
                    ) + fadeIn(tween(220, delayMillis = 80)),
                    exit = shrinkVertically(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        shrinkTowards = Alignment.Top
                    ) + fadeOut(tween(140))
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
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
    ) {
        // 细分隔线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )
        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // 大图标
            Icon(
                imageVector = current.condition.icon(MaterialTheme.colorScheme.onSurface),
                contentDescription = current.condition.label,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(18.dp))

            Column {
                // 大温度
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "${current.temperature}",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "°C",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Text(
                    text = current.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // 高温 / 低温 / 体感
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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

        Spacer(Modifier.height(14.dp))

        // 数据来源标注（小字）
        Text(
            text = "数据更新于 ${MockWeather.UPDATED_AT} · 来源 ${WeatherSource.OPEN_METEO.displayName}",
            style = MaterialTheme.typography.labelSmall,
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
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
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
    tint: Color
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
                strokeLineWidth = 2.2f,
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
            .size(20.dp)
            .clip(RoundedCornerShape(50))
            .rotate(rotation),
        tint = tint
    )
}