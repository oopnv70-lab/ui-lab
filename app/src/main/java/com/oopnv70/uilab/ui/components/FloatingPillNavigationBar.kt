package com.oopnv70.uilab.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oopnv70.uilab.glass.GlassDefaults
import com.oopnv70.uilab.glass.LiquidGlassSurface

// =====================================================================
// 尺寸常量（集中管理，方便整体调节紧凑度）
// =====================================================================
/** 胶囊栏高度。带文字标签时略高。 */
private val BarHeight: Dp = 64.dp
/** 胶囊栏内部左右内边距。 */
private val BarInnerPadding: Dp = 6.dp
/** 滑块相对每个格子的水平内缩（左右各一半 → 共 8dp）。 */
private val IndicatorInset: Dp = 4.dp
/** 滑块相对栏高的垂直内缩。 */
private val IndicatorVerticalInset: Dp = 5.dp

/**
 * 浮动胶囊导航栏（Floating Pill Navigation Bar）。
 *
 * 设计要点：
 *  - **完全胶囊**：圆角 = 高度 / 2，上下两端呈半圆
 *  - **紧凑**：内部留白克制
 *  - **平滑滑动**：选中高亮是一层独立的「滑块」，会从旧位置平滑移动到新位置，
 *    而不是在新位置直接出现（[animateDpAsState] 驱动 `offset`）
 *  - **图标 + 文字**：文字随选中状态渐变出现，兼顾紧凑与可读
 *  - 使用 M3 主题色
 *  - **液态玻璃**：[liquidGlass] 为 true 且系统支持时，胶囊本身改由
 *    [LiquidGlassSurface] 承托（真·折射），不再画实心底
 *
 * ⚠️ 关于玻璃模式的层级（这里很容易搞错）：
 *    玻璃必须能"看见"它身后的天气页内容，所以**胶囊自己的实心底必须消失**。
 *    绘制顺序是：玻璃层（贴在最底）→ 滑块 → 图标/文字。
 *    如果保留 Surface 的实心 color，玻璃抓住的就是那层纯色，等于白做。
 *
 * @param items 导航项目列表。
 * @param selectedIndex 当前选中项下标。
 * @param onSelect 选中回调。
 * @param modifier 外部修饰符（一般用来控制外边距与位置）。
 * @param liquidGlass 是否启用真·液态玻璃胶囊。
 */
@Composable
fun FloatingPillNavigationBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    liquidGlass: Boolean = false
) {
    // 滑块滑动的动画时长。稍长一点，滑动更有「惯性感」。
    val slideDuration = 380

    // 玻璃只在 API 31+ 可用（RuntimeShader 门槛），否则自动退回实心。
    val useGlass = liquidGlass && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val pillShape = RoundedCornerShape(BarHeight / 2)   // 完全胶囊

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight),
        shape = pillShape,
        // 玻璃模式下不填色：让身后的内容透出来给 GlassView 抓。
        // 用 Transparent 而不是删掉 color —— Surface 需要它来决定内容色域。
        color = if (useGlass) Color.Transparent
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = if (useGlass) 0.dp else 2.dp,
        shadowElevation = if (useGlass) 0.dp else 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            // ---------- 最底层：液态玻璃 ----------
            // 声明在最前 → 画在最下，图标与滑块都浮在它上面。
            if (useGlass) {
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    cornerRadiusDp = 999f,             // 胶囊
                    refract = GlassDefaults.REFRACT,
                    curve = GlassDefaults.CURVE,
                    chroma = GlassDefaults.CHROMA,
                    tint = GlassDefaults.TINT,
                    backdropBlur = 0f
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()                          // 撑满胶囊高度，保证滑块垂直居中
                    .padding(horizontal = BarInnerPadding)
            ) {
                val itemCount = items.size.coerceAtLeast(1)
                // 每个 item 的宽度（等分）
                val itemWidth: Dp = maxWidth / itemCount
                // 滑块宽度：略小于 item 宽度，形成「胶囊块」而不是整格
                val indicatorWidth: Dp = itemWidth - IndicatorInset * 2

                // ---------- 滑块的水平位置（核心：这里产生「滑动」效果） ----------
                val targetOffset: Dp = itemWidth * selectedIndex + IndicatorInset
                val indicatorOffset by animateDpAsState(
                    targetValue = targetOffset,
                    animationSpec = tween(
                        durationMillis = slideDuration,
                        easing = FastOutSlowInEasing
                    ),
                    label = "pillIndicatorOffset"
                )

                // ---------- 滑块背景（独立一层，位于图标之下） ----------
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = indicatorOffset)
                        .width(indicatorWidth)
                        .fillMaxHeight()
                        .padding(vertical = IndicatorVerticalInset)
                        .clip(RoundedCornerShape(50))              // 完全胶囊
                        .background(
                            // 玻璃模式下选中块也要半透明，否则会挡住折射
                            if (useGlass) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        )
                )

                // ---------- 图标 + 文字层 ----------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        PillNavItem(
                            item = item,
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) },
                            modifier = Modifier.width(itemWidth)
                        )
                    }
                }
            }
        }
    }
}

/** 单个导航项：图标 + 文字（文字在选中时更醒目）。 */
@Composable
private fun PillNavItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    // 图标颜色：选中用高对比色，未选中用次级色
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 300),
        label = "pillContentColor"
    )

    // 图标缩放：选中略放大，营造弹性感
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "pillIconScale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()                    // 热区高度 = 栏高
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null               // 背景由滑块统一绘制，去掉默认涟漪
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                tint = contentColor
            )
            Box(modifier = Modifier.height(2.dp))
            Text(
                text = item.label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

/** 导航项数据。 */
data class NavItem(
    val icon: ImageVector,
    val label: String
)