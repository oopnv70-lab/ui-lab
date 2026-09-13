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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.oopnv70.uilab.ui.components.FloatingPillNavigationBar
import com.oopnv70.uilab.ui.components.NavItem
import com.oopnv70.uilab.ui.screens.PlaceholderScreen

/**
 * 导航项定义。
 * 名称（label）暂未最终确定，这里先给出占位名，仅用于无障碍描述；
 * 界面上暂不显示文字。
 */
private enum class LabTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("首页", Icons.Rounded.Home, Icons.Outlined.Home),
    EXPLORE("发现", Icons.Rounded.Explore, Icons.Outlined.Explore),
    SAVED("收藏", Icons.Rounded.Bookmark, Icons.Outlined.BookmarkBorder),
    PROFILE("我的", Icons.Rounded.Person, Icons.Outlined.Person)
}

/**
 * 应用主框架：
 *  - 中间是内容区（随 Tab 切换，带淡入淡出过渡）
 *  - 底部是浮动胶囊导航栏（四周留空隙，不贴边、不接地）
 */
@Composable
fun LabApp() {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = LabTab.entries

    // 导航栏需要避让系统导航栏（手势条），再额外留出「悬浮空隙」
    val navBottomInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {

        // ---------- 内容区 ----------
        AnimatedContent(
            targetState = selectedIndex,
            transitionSpec = {
                fadeIn(tween(220)) togetherWith fadeOut(tween(140))
            },
            label = "tabContent"
        ) { index ->
            PlaceholderScreen(
                title = tabs[index].label,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ---------- 浮动胶囊导航栏 ----------
        FloatingPillNavigationBar(
            items = remember {
                tabs.map { NavItem(icon = it.selectedIcon, label = it.label) }
            },
            selectedIndex = selectedIndex,
            onSelect = { selectedIndex = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // 左右 16dp 空隙（跟随 RTL 自动镜像）
                .padding(
                    start = 16.dp,
                    end = 16.dp
                )
                // 底部：系统导航栏高度 + 额外 16dp 悬浮空隙
                .padding(bottom = navBottomInset + 16.dp)
        )
    }
}