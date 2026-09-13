# ui-lab

> 一个 **多源融合天气 App**，用 **Jetpack Compose + Material Design 3** 实现。

[![Build APK](https://github.com/oopnv70-lab/ui-lab/actions/workflows/build.yml/badge.svg)](https://github.com/oopnv70-lab/ui-lab/actions/workflows/build.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](./LICENSE)

---

## 这是什么

一个 **天气应用** —— 但它不是简单地从某一个接口把数据搬过来显示。

它从多个互相独立的天气数据源取数，**在加载时先验证每个源**（能不能通、数据新不新、返回快不快、字段全不全、几个源之间是否互相吻合），再按验证结果**择优采用**，而不是把几家的数据盲拼在一起。每个字段只交给最擅长它的那个源，避免“东拼西凑出一个自相矛盾的天气”。

界面部分全部自研，不依赖第三方 UI 库：

- **灵动岛**（Dynamic Island）—— 收起 / 展开自适应，宽度从左侧锚定生长，尺寸与圆角动画时长统一
- **下拉刷新**（Pull-to-Refresh）
- **零依赖图表层**（降水概率柱状图等，直接用 Compose 绘制）
- **Material 3 完整色彩角色**（26 个 Color Roles）+ **动态取色（Monet）**
- **Material 3 完整排版系统**（15 级 Type Scale，严格遵循官方数值）
- **Material 3 形状系统**（5 级 Shape Scale）
- **边到边显示**（Edge-to-Edge）
- **深色模式**自动跟随系统
- **浮动胶囊导航栏**（Floating Pill Navigation Bar）—— 圆角、悬浮、四周留空隙

### 数据源

| 数据 | 来源 | 说明 |
|---|---|---|
| 实时温度 / 体感 | 多源交叉 | 官方数据源之间实测差异很小，用交叉验证提高可信度 |
| 天气现象（晴 / 雨 / ……） | MET Norway | 使用 `symbol_code`，比 WMO 数字码更细 |
| 逐时预报 / 降水概率 | Open-Meteo | 逐小时粒度完整 |
| 7 天预报 | Open-Meteo | 日级聚合 |
| 空气质量（AQI） | Open-Meteo Air-Quality | |
| 城市搜索 | Open-Meteo Geocoding | 支持多语言地名 |

> 所有数据源均为**免 API key** 的公开服务（Open-Meteo、MET Norway 等）。
> 多源仲裁与择优逻辑仍在推进中，上表为目标职责划分。

### 图标

天气图标有两套并存：

- 项目**自绘**的一套（`WeatherIcons.kt`，描边风格），当前界面在用；
- **Material Design Icons** 一套（`MdiWeatherIcons.kt`，填充风格），作为补充。

MDI 图标取自 [MaterialDesign-SVG](https://github.com/Templarian/MaterialDesign-SVG)，许可证为 **Apache 2.0 + Pictogrammers Free License**（明确 GPL friendly）。图标路径由脚本从 SVG 自动转换，非手工编写。

---

## 界面结构

```
┌─────────────────────────────────┐
│                                 │
│         （内容区）                │
│      带淡入淡出过渡切换            │
│                                 │
│                                 │
│    ╭───────────────────────╮    │  ← 浮动胶囊
│    │  ⌂    ⊙    ⚑    ☺    │    │     右侧图标会高亮
│    ╰───────────────────────╯    │
│              ↑ 距底部 16dp        │
└─────────────────────────────────┘
```

**导航栏设计规格**：

| 项 | 值 |
|---|---|
| 高度 | 64dp（**完全胶囊**：圆角 32dp） |
| 左右空隙 | 16dp |
| 底部空隙 | 16dp（+ 系统导航栏高度） |
| 选中高亮 | `secondaryContainer` 胶囊背景 |
| 图标尺寸 | 24dp（点击热区 48dp） |
| 切换动画 | 250ms `tween`（颜色 / 缩放 / 宽度） |

> ⚠️ 导航项的**名称暂未确定**，界面上暂不显示文字，仅保留图标。

---

## 构建

项目通过 **GitHub Actions** 云端构建，无需本地 Android SDK。

- 触发：推送到 `main` 分支，或手动 `workflow_dispatch`
- 产物：`apk` Artifact（含 debug / release 两个 APK）

### 技术栈

| 项 | 版本 |
|---|---|
| compileSdk / targetSdk | **37**（Android 17）+ `compileSdkMinor = 0` |
| minSdk | 26 |
| AGP | **9.4.0**（内置 Kotlin，无需 `kotlin.android` 插件） |
| Kotlin | 2.2.10 |
| Compose BOM | 2026.09.00 |
| JDK | 17 |

---

## 许可

本项目采用 **GNU General Public License v3.0**，全文见 [LICENSE](./LICENSE)。

> 仅供学习与安全研究使用。
