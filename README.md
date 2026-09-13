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

---

## 主要功能

### 天气数据

| 功能 | 说明 | 状态 |
|---|---|---|
| **真实天气数据** | 从公开气象服务实时拉取，**不编造数字**；字段缺失时显示 `—` 而不是填一个假值 | ✅ 可用 |
| **定位 + 城市搜索** | 自动定位当前城市，也支持手动搜索切换城市 | ✅ 可用 |
| **逐时预报** | 未来 24 小时逐小时温度与天气现象 | ✅ 可用 |
| **7 天预报** | 未来一周的最高 / 最低温 | ✅ 可用 |
| **降水概率** | 未来 24 小时逐小时降水概率，附中文人话说明（"记得带伞"之类） | ✅ 可用 |
| **空气质量** | AQI 指数及等级 | ✅ 可用 |
| **多源融合** | 多源取数 → 加载时验证 → 择优采用 | 🚧 进行中 |

### 界面

| 功能 | 说明 | 状态 |
|---|---|---|
| **灵动岛** | 顶部胶囊，收起 / 展开自适应；宽度**从左侧锚定**生长，尺寸与圆角动画时长统一，无撕裂感 | ✅ 可用 |
| **下拉刷新** | 下拉手势重新拉取天气 | ✅ 可用 |
| **零依赖图表层** | 降水概率柱状图等直接由 Compose 绘制，不引入图表库 | ✅ 可用 |
| **浮动胶囊导航栏** | 悬浮圆角胶囊，四周留空隙，选中项高亮 | ✅ 可用 |
| **深色模式** | 自动跟随系统 | ✅ 可用 |
| **动态取色（Monet）** | 从壁纸取色，Android 12+ 生效 | ✅ 可用 |
| **边到边显示** | Edge-to-Edge，内容延伸到状态栏 / 导航栏下方 | ✅ 可用 |

---

## 界面结构

```
┌─────────────────────────────────┐
│      ╭──────────────╮           │  ← 灵动岛
│      │  ☀  24°  多云 │           │
│      ╰──────────────╯           │
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
- 产物：`ui-lab-debug-apk` Artifact（**只有 debug APK**；release 当前未在 CI 产出）
- 签名：使用**固定签名**，APK 可直接覆盖安装升级（无需卸载）

> 📄 **完整的构建配置说明见 [BUILD.md](./BUILD.md)** —— 包含逐项版本号、
> 固定签名的实现方式、AGP 9.x 的两个易踩坑点、以及 CI 排错提示。
> 该文档中的每一条都与仓库真实文件逐项核对过。

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

## 第三方资源与署名

### 1. Material Design Icons（天气图标）

文件：`app/src/main/java/com/oopnv70/uilab/ui/weather/MdiWeatherIcons.kt`

图标集：[Material Design Icons (MDI)](https://materialdesignicons.com/) —— 原仓库 [Templarian/MaterialDesign-SVG](https://github.com/Templarian/MaterialDesign-SVG)
作者：**Pictogrammers**（社区维护，源自 Google Material Design 图标计划）
下载地址：[https://github.com/Templarian/MaterialDesign-SVG/tree/master/svg](https://github.com/Templarian/MaterialDesign-SVG/tree/master/svg)
许可证：**Apache License 2.0** + **Pictogrammers Free License**（明确 GPL friendly，与本项目 GPL-3.0 兼容）

项目用到 **10 个**图标，逐一署名如下：

| 用途 | 图标名 | 原始文件 |
|---|---|---|
| 晴 | `weather-sunny` | [svg/weather-sunny.svg](https://github.com/Templarian/MaterialDesign-SVG/blob/master/svg/weather-sunny.svg) |
| 多云 | `weather-cloudy` | [svg/weather-cloudy.svg](https://github.com/Templarian/MaterialDesign-SVG/blob/master/svg/weather-cloudy.svg) |
| 晴间多云 | `weather-partly-cloudy` | [svg/weather-partly-cloudy.svg](https://github.com/Templarian/MaterialDesign-SVG/blob/master/svg/weather-partly-cloudy.svg) |
| 雨 | `weather-rainy` | [svg/weather-rainy.svg](https://github.com/Templarian/MaterialDesign-SVG/blob/master/svg/weather-rainy.svg) |
| 大雨 / 倾盆 | `weather-pouring` | [svg/weather-pouring.svg](https://github.com/Templarian/MaterialDesign-SVG/blob/master/svg/weather-pouring.svg) |
| 雪 | `weather-snowy` | [svg/weather-snowy.svg](https://github.com/Templarian/MaterialDesign-SVG/blob/master/svg/weather-snowy.svg) |
| 雷 | `weather-lightning` | [svg/weather-lightning.svg](https://github.com/Templarian/MaterialDesign-SVG/blob/master/svg/weather-lightning.svg) |
| 雾 | `weather-fog` | [svg/weather-fog.svg](https://github.com/Templarian/MaterialDesign-SVG/blob/master/svg/weather-fog.svg) |
| 风 | `weather-windy` | [svg/weather-windy.svg](https://github.com/Templarian/MaterialDesign-SVG/blob/master/svg/weather-windy.svg) |
| 夜间 | `weather-night` | [svg/weather-night.svg](https://github.com/Templarian/MaterialDesign-SVG/blob/master/svg/weather-night.svg) |

原始 SVG 内容（可直接对照验证）：

```
<svg xmlns="http://www.w3.org/2000/svg" id="mdi-weather-sunny" viewBox="0 0 24 24">
  <path d="M12,7A5,5 0 0,1 17,12A5,5 0 0,1 12,17A5,5 0 0,1 7,12A5,5 0 0,1 12,7M12,9A3,3 ..."/>
</svg>
```

**转换说明**：上述图标并非手工绘制，而是由脚本解析 SVG 的 `d` 路径属性、机械转换为 Compose 的 `ImageVector.Builder` 调用（`moveTo` / `lineTo` / `curveTo` / `arcTo` / `close`），因此形状与原始 SVG 完全一致。每个函数上方的注释保留了原始图标名。全部 10 个图标均保持 `viewBox 0 0 24 24` 原始坐标，未做变形。

### 2. 项目自绘图标

文件：`app/src/main/java/com/oopnv70/uilab/ui/weather/WeatherIcons.kt`

这套图标为**本项目原创**（用 Compose `ImageVector.Builder` 描边绘制），采用与项目源码相同的 GPL-3.0 许可，无第三方权利限制。

### 3. 天气数据源

详见上方 [数据源](#数据源) 表格。所有服务均为公开、免 API key 的气象数据接口，使用时请遵守各自的使用条款（如 MET Norway 要求标明数据来源、Open-Meteo 要求注明非商业用途限制）。

---

## 许可

本项目采用 **GNU General Public License v3.0**，全文见 [LICENSE](./LICENSE)。

> 仅供学习与安全研究使用。
