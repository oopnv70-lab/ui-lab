package com.oopnv70.uilab.ui.theme

import androidx.compose.ui.graphics.Color

// =====================================================================
// Material 3 基准色板（Baseline Color Scheme）
// 参考：https://m3.material.io/styles/color/roles
// 这是官方为「动态取色不可用」的设备提供的 fallback 配色。
// 动态取色（Monet，Android 12+）可用时会自动覆盖这些值。
// =====================================================================

// ---------- Light ----------
val PrimaryLight = Color(0xFF415F91)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFD6E3FF)
val OnPrimaryContainerLight = Color(0xFF001B3E)

val SecondaryLight = Color(0xFF565F71)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFDAE2F9)
val OnSecondaryContainerLight = Color(0xFF131C2B)

val TertiaryLight = Color(0xFF705575)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFAD8FD)
val OnTertiaryContainerLight = Color(0xFF28132E)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val BackgroundLight = Color(0xFFFCFCFF)
val OnBackgroundLight = Color(0xFF191C20)
val SurfaceLight = Color(0xFFFCFCFF)
val OnSurfaceLight = Color(0xFF191C20)
val SurfaceVariantLight = Color(0xFFE4E6F0)
val OnSurfaceVariantLight = Color(0xFF44474E)

val OutlineLight = Color(0xFF74777F)
val OutlineVariantLight = Color(0xFFC4C6D0)
val ScrimLight = Color(0xFF000000)

val InverseSurfaceLight = Color(0xFF2E3036)
val InverseOnSurfaceLight = Color(0xFFF0F0F7)
val InversePrimaryLight = Color(0xFFAAC7FF)

// ---------- Surface Containers（浅色：整体提亮，用于导航栏等「浮起」容器） ----------
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF6F7FC)
val SurfaceContainerLight = Color(0xFFF1F2F8)
val SurfaceContainerHighLight = Color(0xFFEBEDF3)
val SurfaceContainerHighestLight = Color(0xFFE5E7EE)

// ---------- Dark ----------
val PrimaryDark = Color(0xFFAAC7FF)
val OnPrimaryDark = Color(0xFF0A305F)
val PrimaryContainerDark = Color(0xFF284777)
val OnPrimaryContainerDark = Color(0xFFD6E3FF)

val SecondaryDark = Color(0xFFBEC6DC)
val OnSecondaryDark = Color(0xFF283141)
val SecondaryContainerDark = Color(0xFF3E4759)
val OnSecondaryContainerDark = Color(0xFFDAE2F9)

val TertiaryDark = Color(0xFFDDBCE0)
val OnTertiaryDark = Color(0xFF3F2844)
val TertiaryContainerDark = Color(0xFF573E5C)
val OnTertiaryContainerDark = Color(0xFFFAD8FD)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val BackgroundDark = Color(0xFF111318)
val OnBackgroundDark = Color(0xFFE2E2E9)
val SurfaceDark = Color(0xFF111318)
val OnSurfaceDark = Color(0xFFE2E2E9)
val SurfaceVariantDark = Color(0xFF44474E)
val OnSurfaceVariantDark = Color(0xFFC4C6D0)

val OutlineDark = Color(0xFF8E9099)
val OutlineVariantDark = Color(0xFF44474E)
val ScrimDark = Color(0xFF000000)

val InverseSurfaceDark = Color(0xFFE2E2E9)
val InverseOnSurfaceDark = Color(0xFF2E3036)
val InversePrimaryDark = Color(0xFF415F91)

// ---------- Surface Containers（深色） ----------
val SurfaceContainerLowestDark = Color(0xFF0C0E13)
val SurfaceContainerLowDark = Color(0xFF191C20)
val SurfaceContainerDark = Color(0xFF1D2024)
val SurfaceContainerHighDark = Color(0xFF272A2F)
val SurfaceContainerHighestDark = Color(0xFF32353A)