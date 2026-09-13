package com.oopnv70.uilab.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oopnv70.uilab.settings.AppSettingsState
import com.oopnv70.uilab.settings.TemperatureUnit
import com.oopnv70.uilab.settings.ThemeMode
import com.oopnv70.uilab.settings.ThemeStyle
import com.oopnv70.uilab.ui.weather.mdiClose
import com.oopnv70.uilab.ui.weather.mdiCog

// =====================================================================
// 设置页
// =====================================================================
// 设计目标（对齐本项目已有的视觉语言）：
//   - 与 WeatherPages 用同一套「卡片 + 分节标题」结构（圆角 20dp、surfaceContainerHigh）
//   - 顶部一条标题栏（含关闭按钮），底部留导航栏高度
//   - 每一项都是「现在真的生效」的，不放占位开关
//
// 关于「哪些项该放进来」的判断标准：
//   一个设置项只有在「改完之后 UI 真的会变」时才配存在。
//   放一个点了没反应的开关，比没有设置页更糟 —— 用户会以为 App 坏了。
//   因此本次只放：主题模式、动态取色、温度单位（都会立即生效），
//   加上两块只读信息（数据源 / 关于），它们不假装可交互。
// =====================================================================

/** 设置页所有可选的分节。 */
private val ThemeModes = ThemeMode.entries.toList()
private val TemperatureUnits = TemperatureUnit.entries.toList()
private val ThemeStyles = ThemeStyle.entries.toList()

/**
 * 「风格」这一行的说明文字。
 *
 * 特意把「实时渲染」写出来：用户选玻璃之前应该知道它会持续占用 GPU，
 * 而不是选完才发现掉帧。诚实说明比事后道歉便宜。
 */
private const val SettingsStylesDescription =
    "默认：实心卡片；液态玻璃：真折射 + 高光，实时渲染（较耗性能）"

/**
 * 设置页（全屏覆盖）。
 *
 * @param settings 当前设置快照。
 * @param onSettingsChange 用户改动设置后的回调（调用方负责落盘 + 更新状态）。
 * @param onClose 关闭设置页。
 * @param appVersion 版本号（展示用，由调用方传入，避免这里再读 BuildConfig）。
 */
@Composable
fun SettingsPage(
    settings: AppSettingsState,
    onSettingsChange: (AppSettingsState) -> Unit,
    onClose: () -> Unit,
    appVersion: String = ""
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 设置页自己的背景色：用 surfaceContainerLowest，比内容区略深一点，
    // 形成「上层面板」的视觉层次（全屏覆盖时不会与下面的天气页混淆）。
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ---------- 顶部标题栏 ----------
            SettingsTopBar(
                topPadding = statusBarTop,
                onClose = onClose
            )

            // ---------- 可滚动内容 ----------
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    // 底部多留：避免最后一项被系统导航栏遮住
                    bottom = navBottomInset + 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ---- 外观 ----
                item { SectionLabel("外观") }
                item {
                    SettingsCard {
                        // 主题模式：三选一（分段式，不弹对话框）
                        ChoiceRow(
                            title = "主题",
                            subtitle = "深色 / 浅色，或跟随系统",
                            options = ThemeModes.map { it.label },
                            selectedIndex = ThemeModes.indexOf(settings.themeMode),
                            onSelect = { index ->
                                onSettingsChange(settings.copy(themeMode = ThemeModes[index]))
                            }
                        )
                        CardDivider()
                        // 主题风格：默认实心卡片 / 液态玻璃
                        //
                        // 这是与「主题」正交的第二个维度：主题管亮暗，
                        // 风格管材质。所以不合并成一个下拉。
                        ChoiceRow(
                            title = "风格",
                            subtitle = SettingsStylesDescription,
                            options = ThemeStyles.map { it.label },
                            selectedIndex = ThemeStyles.indexOf(settings.themeStyle),
                            onSelect = { index ->
                                onSettingsChange(settings.copy(themeStyle = ThemeStyles[index]))
                            }
                        )
                        CardDivider()
                        // 动态取色：开关
                        SwitchRow(
                            title = "动态取色",
                            subtitle = "跟随壁纸生成配色（Android 12+）。" +
                                "开启后会覆盖本项目自带的配色",
                            checked = settings.dynamicColor,
                            onCheckedChange = { checked ->
                                onSettingsChange(settings.copy(dynamicColor = checked))
                            }
                        )
                    }
                }

                // ---- 单位 ----
                item { SectionLabel("单位") }
                item {
                    SettingsCard {
                        ChoiceRow(
                            title = "温度单位",
                            subtitle = "影响界面上所有温度数值的显示",
                            options = TemperatureUnits.map { it.label + "（${it.suffix}）" },
                            selectedIndex = TemperatureUnits.indexOf(settings.temperatureUnit),
                            onSelect = { index ->
                                onSettingsChange(
                                    settings.copy(temperatureUnit = TemperatureUnits[index])
                                )
                            }
                        )
                    }
                }

                // ---- 数据来源（只读） ----
                item { SectionLabel("数据来源") }
                item {
                    SettingsCard {
                        InfoRow(
                            title = "天气数据",
                            value = "Open-Meteo / MET Norway"
                        )
                        CardDivider()
                        InfoRow(
                            title = "城市搜索",
                            value = "Photon / Open-Meteo"
                        )
                        CardDivider()
                        InfoRow(
                            title = "定位与逆地理",
                            value = "系统定位服务"
                        )
                    }
                }

                // ---- 关于（只读） ----
                item { SectionLabel("关于") }
                item {
                    SettingsCard {
                        InfoRow(title = "应用", value = "ui-lab")
                        if (appVersion.isNotBlank()) {
                            CardDivider()
                            InfoRow(title = "版本", value = appVersion)
                        }
                        CardDivider()
                        InfoRow(title = "开源协议", value = "GPL-3.0")
                        CardDivider()
                        InfoRow(
                            title = "源码",
                            value = "github.com/oopnv70-lab/ui-lab"
                        )
                    }
                }

                // 底部一句说明，避免最后一块卡片贴着屏幕边缘
                item {
                    Text(
                        text = "设置项即时生效，已保存在本机。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

// =====================================================================
// 顶部标题栏
// =====================================================================

/** 设置页顶部：标题 + 关闭按钮。 */
@Composable
private fun SettingsTopBar(topPadding: androidx.compose.ui.unit.Dp, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 8.dp,
                end = 16.dp,
                top = topPadding + 8.dp,
                bottom = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 关闭按钮：圆形点击区，视觉上轻一些（不抢标题的注意力）
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = mdiClose(strokeColor()),
                contentDescription = "关闭设置",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "设置",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// =====================================================================
// 通用卡片 / 分节
// =====================================================================

/** 分节小标题（与 WeatherPages 的 SectionTitle 同风格，但不带 trailing）。 */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
    )
}

/** 设置卡片容器：圆角 + 容器色，与天气卡片视觉统一，内部垂直排布。 */
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/** 卡片内分隔线（故意做得很轻，只用极低对比度的底色，不用 Divider 组件）。 */
@Composable
private fun CardDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    )
}

// =====================================================================
// 具体行类型
// =====================================================================

/**
 * 分段选择行：标题 + 副标题 + 一排可点的选项。
 *
 * 为什么不用 DropdownMenu：
 *   设置项只有 2~3 个选项，摊开成「分段控件」一眼能看全，
 *   比「点开菜单再选」少一步操作，也更适合天气 App 这种轻量场景。
 */
@Composable
private fun ChoiceRow(
    title: String,
    subtitle: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        RowTexts(title = title, subtitle = subtitle)

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEachIndexed { index, label ->
                SegmentChip(
                    text = label,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** 单个分段按钮。选中态用 primaryContainer 填充，未选中用透明 + 描边。 */
@Composable
private fun SegmentChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1
            )
        }
    }
}

/** 开关行：标题 + 副标题 + 右侧 Switch。整行可点（点哪都能切）。 */
@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            RowTexts(title = title, subtitle = subtitle)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            // 整行已处理点击，Switch 自身不需要再触发一次（否则会双触发变回原值）
            onCheckedChange = null
        )
    }
}

/** 只读信息行：左侧标题，右侧值（值右对齐、用次要色）。 */
@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** 标题 + 副标题的公共排版。 */
@Composable
private fun RowTexts(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * 图标描边色。
 *
 * 与 buildNavItems() 同样的道理：自绘/转换图标在构建 ImageVector 时
 * 已把 SolidColor(tint) 固化进路径，传 Color.Unspecified 会画不出来。
 * 这里给一个确定实色，真正的显示颜色由 Icon(tint = ...) 的 ColorFilter 覆盖。
 */
private fun strokeColor(): Color = Color.Black