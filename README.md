# ui-lab

> 一个 **Jetpack Compose + 标准 Material Design 3** 实现的 Android 界面项目。

[![Build APK](https://github.com/oopnv70-lab/ui-lab/actions/workflows/build.yml/badge.svg)](https://github.com/oopnv70-lab/ui-lab/actions/workflows/build.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](./LICENSE)

---

## 这是什么

一个以 **「标准 Material Design」** 为目标实现的 Android 示例应用：

- **Material 3 完整色彩角色**（26 个 Color Roles）+ **动态取色（Monet）**
- **Material 3 完整排版系统**（15 级 Type Scale，严格遵循官方数值）
- **Material 3 形状系统**（5 级 Shape Scale）
- **边到边显示**（Edge-to-Edge）
- **深色模式**自动跟随系统
- **浮动胶囊导航栏**（Floating Pill Navigation Bar）—— 圆角、悬浮、四周留空隙

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
