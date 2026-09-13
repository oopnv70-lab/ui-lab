package com.oopnv70.uilab.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * 浮动胶囊导航栏（Floating Pill Navigation Bar）。
 *
 * 设计要点：
 *  - 大圆角（完全胶囊：高 64dp，圆角 32dp）
 *  - 四周留空隙，不贴合屏幕边缘（左右 16dp、底部 16dp）
 *  - 选中项带「胶囊高亮」背景（现代主流观感）
 *  - 使用 M3 主题色（配合动态取色）
 *  - 图标带弹性缩放动画；图标本身（[Icon]）为 24dp，但点击热区 ≥ 48dp
 *
 * @param items 导航项目列表。
 * @param selectedIndex 当前选中项下标。
 * @param onSelect 选中回调。
 * @param modifier 外部修饰符（一般用来控制外边距与位置）。
 */
@Composable
fun FloatingPillNavigationBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),                 // 胶囊高度：64dp → 圆角 32dp = 完全胶囊
        shape = RoundedCornerShape(32.dp),  // 完全胶囊
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                PillNavItem(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** 单个导航项：未选中为透明，选中为胶囊高亮 + 图标轻微放大。 */
@Composable
private fun PillNavItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    // 选中胶囊背景色
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 250),
        label = "pillContainerColor"
    )

    // 图标/文字颜色
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 250),
        label = "pillContentColor"
    )

    // 图标缩放：选中略放大（1.0 → 1.06），营造弹性感
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1.0f,
        animationSpec = tween(durationMillis = 250),
        label = "pillIconScale"
    )

    // 选中胶囊的宽度
    val pillHorizontalPadding by animateDpAsState(
        targetValue = if (selected) 20.dp else 12.dp,
        animationSpec = tween(durationMillis = 250),
        label = "pillPadding"
    )

    Box(
        modifier = modifier
            .height(48.dp)                       // 点击热区高度 ≥ 48dp（M3 可访问性要求）
            .clip(RoundedCornerShape(24.dp))
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null                 // 自绘背景，去掉默认涟漪
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(containerColor)
                .padding(horizontal = pillHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                    tint = contentColor
                )
                // 说明：选项名称暂时不显示，待命名确定后在此追加 Text。
            }
        }
    }
}

/** 导航项数据。 */
data class NavItem(
    val icon: ImageVector,
    val label: String
)