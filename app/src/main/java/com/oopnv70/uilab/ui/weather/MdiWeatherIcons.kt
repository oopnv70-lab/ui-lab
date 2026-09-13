package com.oopnv70.uilab.ui.weather

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// =====================================================================
// Material Design Icons (MDI) 天气图标
// =====================================================================
// 这些图标不是手绘的，而是取自 Material Design Icons 官方图形资源，
// 由脚本根据 SVG path 的 d 属性自动转换而来（保证形状与源文件一致）。
//
// 来源：https://github.com/Templarian/MaterialDesign-SVG
// 许可证：Apache License 2.0 + Pictogrammers Free License
//   - 允许商用、修改、再分发；
//   - 要求保留版权与许可声明（即本注释块）；
//   - 明确 GPL-friendly，与本项目 GPL-3.0 兼容。
//
// 用途：作为 MET Norway symbol_code 等现象码的补充图标集。
// 现有自绘图标（WeatherIcons.kt）保持不动，两套并存。
//
// 规格：viewBox 0 0 24 24（与现有图标的 24x24 体系一致），
//       fill 填充式（现有自绘为 stroke 描边式）。
//
// 本文件由 /tmp/svg2compose.py 生成后加注释头，请勿手工逐行改路径。
// =====================================================================

/** weather-cloudy（来源：Material Design Icons，Apache 2.0 / Pictogrammers Free License）。 */
fun mdiWeatherCloudy(tint: Color): ImageVector = ImageVector.Builder(
    name = "weather-cloudy",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(tint)) {
        moveTo(6f, 19f)
        arcTo(5f, 5f, 0f, false, true, 1f, 14f)
        arcTo(5f, 5f, 0f, false, true, 6f, 9f)
        curveTo(7f, 6.65f, 9.3f, 5f, 12f, 5f)
        curveTo(15.43f, 5f, 18.24f, 7.66f, 18.5f, 11.03f)
        lineTo(19f, 11f)
        arcTo(4f, 4f, 0f, false, true, 23f, 15f)
        arcTo(4f, 4f, 0f, false, true, 19f, 19f)
        horizontalLineTo(6f)
        moveTo(19f, 13f)
        horizontalLineTo(17f)
        verticalLineTo(12f)
        arcTo(5f, 5f, 0f, false, false, 12f, 7f)
        curveTo(9.5f, 7f, 7.45f, 8.82f, 7.06f, 11.19f)
        curveTo(6.73f, 11.07f, 6.37f, 11f, 6f, 11f)
        arcTo(3f, 3f, 0f, false, false, 3f, 14f)
        arcTo(3f, 3f, 0f, false, false, 6f, 17f)
        horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, false, 21f, 15f)
        arcTo(2f, 2f, 0f, false, false, 19f, 13f)
        close()
    }
}.build()

/** weather-fog（来源：Material Design Icons，Apache 2.0 / Pictogrammers Free License）。 */
fun mdiWeatherFog(tint: Color): ImageVector = ImageVector.Builder(
    name = "weather-fog",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(tint)) {
        moveTo(3f, 15f)
        horizontalLineTo(13f)
        arcTo(1f, 1f, 0f, false, true, 14f, 16f)
        arcTo(1f, 1f, 0f, false, true, 13f, 17f)
        horizontalLineTo(3f)
        arcTo(1f, 1f, 0f, false, true, 2f, 16f)
        arcTo(1f, 1f, 0f, false, true, 3f, 15f)
        moveTo(16f, 15f)
        horizontalLineTo(21f)
        arcTo(1f, 1f, 0f, false, true, 22f, 16f)
        arcTo(1f, 1f, 0f, false, true, 21f, 17f)
        horizontalLineTo(16f)
        arcTo(1f, 1f, 0f, false, true, 15f, 16f)
        arcTo(1f, 1f, 0f, false, true, 16f, 15f)
        moveTo(1f, 12f)
        arcTo(5f, 5f, 0f, false, true, 6f, 7f)
        curveTo(7f, 4.65f, 9.3f, 3f, 12f, 3f)
        curveTo(15.43f, 3f, 18.24f, 5.66f, 18.5f, 9.03f)
        lineTo(19f, 9f)
        curveTo(21.19f, 9f, 22.97f, 10.76f, 23f, 13f)
        horizontalLineTo(21f)
        arcTo(2f, 2f, 0f, false, false, 19f, 11f)
        horizontalLineTo(17f)
        verticalLineTo(10f)
        arcTo(5f, 5f, 0f, false, false, 12f, 5f)
        curveTo(9.5f, 5f, 7.45f, 6.82f, 7.06f, 9.19f)
        curveTo(6.73f, 9.07f, 6.37f, 9f, 6f, 9f)
        arcTo(3f, 3f, 0f, false, false, 3f, 12f)
        curveTo(3f, 12.35f, 3.06f, 12.69f, 3.17f, 13f)
        horizontalLineTo(1.1f)
        lineTo(1f, 12f)
        moveTo(3f, 19f)
        horizontalLineTo(5f)
        arcTo(1f, 1f, 0f, false, true, 6f, 20f)
        arcTo(1f, 1f, 0f, false, true, 5f, 21f)
        horizontalLineTo(3f)
        arcTo(1f, 1f, 0f, false, true, 2f, 20f)
        arcTo(1f, 1f, 0f, false, true, 3f, 19f)
        moveTo(8f, 19f)
        horizontalLineTo(21f)
        arcTo(1f, 1f, 0f, false, true, 22f, 20f)
        arcTo(1f, 1f, 0f, false, true, 21f, 21f)
        horizontalLineTo(8f)
        arcTo(1f, 1f, 0f, false, true, 7f, 20f)
        arcTo(1f, 1f, 0f, false, true, 8f, 19f)
        close()
    }
}.build()

/** weather-lightning（来源：Material Design Icons，Apache 2.0 / Pictogrammers Free License）。 */
fun mdiWeatherLightning(tint: Color): ImageVector = ImageVector.Builder(
    name = "weather-lightning",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(tint)) {
        moveTo(6f, 16f)
        arcTo(5f, 5f, 0f, false, true, 1f, 11f)
        arcTo(5f, 5f, 0f, false, true, 6f, 6f)
        curveTo(7f, 3.65f, 9.3f, 2f, 12f, 2f)
        curveTo(15.43f, 2f, 18.24f, 4.66f, 18.5f, 8.03f)
        lineTo(19f, 8f)
        arcTo(4f, 4f, 0f, false, true, 23f, 12f)
        arcTo(4f, 4f, 0f, false, true, 19f, 16f)
        horizontalLineTo(18f)
        arcTo(1f, 1f, 0f, false, true, 17f, 15f)
        arcTo(1f, 1f, 0f, false, true, 18f, 14f)
        horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, false, 21f, 12f)
        arcTo(2f, 2f, 0f, false, false, 19f, 10f)
        horizontalLineTo(17f)
        verticalLineTo(9f)
        arcTo(5f, 5f, 0f, false, false, 12f, 4f)
        curveTo(9.5f, 4f, 7.45f, 5.82f, 7.06f, 8.19f)
        curveTo(6.73f, 8.07f, 6.37f, 8f, 6f, 8f)
        arcTo(3f, 3f, 0f, false, false, 3f, 11f)
        arcTo(3f, 3f, 0f, false, false, 6f, 14f)
        horizontalLineTo(7f)
        arcTo(1f, 1f, 0f, false, true, 8f, 15f)
        arcTo(1f, 1f, 0f, false, true, 7f, 16f)
        horizontalLineTo(6f)
        moveTo(12f, 11f)
        horizontalLineTo(15f)
        lineTo(13f, 15f)
        horizontalLineTo(15f)
        lineTo(11.25f, 22f)
        lineTo(12f, 17f)
        horizontalLineTo(9.5f)
        lineTo(12f, 11f)
        close()
    }
}.build()

/** weather-night（来源：Material Design Icons，Apache 2.0 / Pictogrammers Free License）。 */
fun mdiWeatherNight(tint: Color): ImageVector = ImageVector.Builder(
    name = "weather-night",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(tint)) {
        moveTo(17.75f, 4.09f)
        lineTo(15.22f, 6.03f)
        lineTo(16.13f, 9.09f)
        lineTo(13.5f, 7.28f)
        lineTo(10.87f, 9.09f)
        lineTo(11.78f, 6.03f)
        lineTo(9.25f, 4.09f)
        lineTo(12.44f, 4f)
        lineTo(13.5f, 1f)
        lineTo(14.56f, 4f)
        lineTo(17.75f, 4.09f)
        moveTo(21.25f, 11f)
        lineTo(19.61f, 12.25f)
        lineTo(20.2f, 14.23f)
        lineTo(18.5f, 13.06f)
        lineTo(16.8f, 14.23f)
        lineTo(17.39f, 12.25f)
        lineTo(15.75f, 11f)
        lineTo(17.81f, 10.95f)
        lineTo(18.5f, 9f)
        lineTo(19.19f, 10.95f)
        lineTo(21.25f, 11f)
        moveTo(18.97f, 15.95f)
        curveTo(19.8f, 15.87f, 20.69f, 17.05f, 20.16f, 17.8f)
        curveTo(19.84f, 18.25f, 19.5f, 18.67f, 19.08f, 19.07f)
        curveTo(15.17f, 23f, 8.84f, 23f, 4.94f, 19.07f)
        curveTo(1.03f, 15.17f, 1.03f, 8.83f, 4.94f, 4.93f)
        curveTo(5.34f, 4.53f, 5.76f, 4.17f, 6.21f, 3.85f)
        curveTo(6.96f, 3.32f, 8.14f, 4.21f, 8.06f, 5.04f)
        curveTo(7.79f, 7.9f, 8.75f, 10.87f, 10.95f, 13.06f)
        curveTo(13.14f, 15.26f, 16.1f, 16.22f, 18.97f, 15.95f)
        moveTo(17.33f, 17.97f)
        curveTo(14.5f, 17.81f, 11.7f, 16.64f, 9.53f, 14.5f)
        curveTo(7.36f, 12.31f, 6.2f, 9.5f, 6.04f, 6.68f)
        curveTo(3.23f, 9.82f, 3.34f, 14.64f, 6.35f, 17.66f)
        curveTo(9.37f, 20.67f, 14.19f, 20.78f, 17.33f, 17.97f)
        close()
    }
}.build()

/** weather-partly-cloudy（来源：Material Design Icons，Apache 2.0 / Pictogrammers Free License）。 */
fun mdiWeatherPartlyCloudy(tint: Color): ImageVector = ImageVector.Builder(
    name = "weather-partly-cloudy",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(tint)) {
        moveTo(12.74f, 5.47f)
        curveTo(15.1f, 6.5f, 16.35f, 9.03f, 15.92f, 11.46f)
        curveTo(17.19f, 12.56f, 18f, 14.19f, 18f, 16f)
        verticalLineTo(16.17f)
        curveTo(18.31f, 16.06f, 18.65f, 16f, 19f, 16f)
        arcTo(3f, 3f, 0f, false, true, 22f, 19f)
        arcTo(3f, 3f, 0f, false, true, 19f, 22f)
        horizontalLineTo(6f)
        arcTo(4f, 4f, 0f, false, true, 2f, 18f)
        arcTo(4f, 4f, 0f, false, true, 6f, 14f)
        horizontalLineTo(6.27f)
        curveTo(5f, 12.45f, 4.6f, 10.24f, 5.5f, 8.26f)
        curveTo(6.72f, 5.5f, 9.97f, 4.24f, 12.74f, 5.47f)
        moveTo(11.93f, 7.3f)
        curveTo(10.16f, 6.5f, 8.09f, 7.31f, 7.31f, 9.07f)
        curveTo(6.85f, 10.09f, 6.93f, 11.22f, 7.41f, 12.13f)
        curveTo(8.5f, 10.83f, 10.16f, 10f, 12f, 10f)
        curveTo(12.7f, 10f, 13.38f, 10.12f, 14f, 10.34f)
        curveTo(13.94f, 9.06f, 13.18f, 7.86f, 11.93f, 7.3f)
        moveTo(13.55f, 3.64f)
        curveTo(13f, 3.4f, 12.45f, 3.23f, 11.88f, 3.12f)
        lineTo(14.37f, 1.82f)
        lineTo(15.27f, 4.71f)
        curveTo(14.76f, 4.29f, 14.19f, 3.93f, 13.55f, 3.64f)
        moveTo(6.09f, 4.44f)
        curveTo(5.6f, 4.79f, 5.17f, 5.19f, 4.8f, 5.63f)
        lineTo(4.91f, 2.82f)
        lineTo(7.87f, 3.5f)
        curveTo(7.25f, 3.71f, 6.65f, 4.03f, 6.09f, 4.44f)
        moveTo(18f, 9.71f)
        curveTo(17.91f, 9.12f, 17.78f, 8.55f, 17.59f, 8f)
        lineTo(19.97f, 9.5f)
        lineTo(17.92f, 11.73f)
        curveTo(18.03f, 11.08f, 18.05f, 10.4f, 18f, 9.71f)
        moveTo(3.04f, 11.3f)
        curveTo(3.11f, 11.9f, 3.24f, 12.47f, 3.43f, 13f)
        lineTo(1.06f, 11.5f)
        lineTo(3.1f, 9.28f)
        curveTo(3f, 9.93f, 2.97f, 10.61f, 3.04f, 11.3f)
        moveTo(19f, 18f)
        horizontalLineTo(16f)
        verticalLineTo(16f)
        arcTo(4f, 4f, 0f, false, false, 12f, 12f)
        arcTo(4f, 4f, 0f, false, false, 8f, 16f)
        horizontalLineTo(6f)
        arcTo(2f, 2f, 0f, false, false, 4f, 18f)
        arcTo(2f, 2f, 0f, false, false, 6f, 20f)
        horizontalLineTo(19f)
        arcTo(1f, 1f, 0f, false, false, 20f, 19f)
        arcTo(1f, 1f, 0f, false, false, 19f, 18f)
        close()
    }
}.build()

/** weather-pouring（来源：Material Design Icons，Apache 2.0 / Pictogrammers Free License）。 */
fun mdiWeatherPouring(tint: Color): ImageVector = ImageVector.Builder(
    name = "weather-pouring",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(tint)) {
        moveTo(9f, 12f)
        curveTo(9.53f, 12.14f, 9.85f, 12.69f, 9.71f, 13.22f)
        lineTo(8.41f, 18.05f)
        curveTo(8.27f, 18.59f, 7.72f, 18.9f, 7.19f, 18.76f)
        curveTo(6.65f, 18.62f, 6.34f, 18.07f, 6.5f, 17.54f)
        lineTo(7.78f, 12.71f)
        curveTo(7.92f, 12.17f, 8.47f, 11.86f, 9f, 12f)
        moveTo(13f, 12f)
        curveTo(13.53f, 12.14f, 13.85f, 12.69f, 13.71f, 13.22f)
        lineTo(11.64f, 20.95f)
        curveTo(11.5f, 21.5f, 10.95f, 21.8f, 10.41f, 21.66f)
        curveTo(9.88f, 21.5f, 9.56f, 20.97f, 9.7f, 20.43f)
        lineTo(11.78f, 12.71f)
        curveTo(11.92f, 12.17f, 12.47f, 11.86f, 13f, 12f)
        moveTo(17f, 12f)
        curveTo(17.53f, 12.14f, 17.85f, 12.69f, 17.71f, 13.22f)
        lineTo(16.41f, 18.05f)
        curveTo(16.27f, 18.59f, 15.72f, 18.9f, 15.19f, 18.76f)
        curveTo(14.65f, 18.62f, 14.34f, 18.07f, 14.5f, 17.54f)
        lineTo(15.78f, 12.71f)
        curveTo(15.92f, 12.17f, 16.47f, 11.86f, 17f, 12f)
        moveTo(17f, 10f)
        verticalLineTo(9f)
        arcTo(5f, 5f, 0f, false, false, 12f, 4f)
        curveTo(9.5f, 4f, 7.45f, 5.82f, 7.06f, 8.19f)
        curveTo(6.73f, 8.07f, 6.37f, 8f, 6f, 8f)
        arcTo(3f, 3f, 0f, false, false, 3f, 11f)
        curveTo(3f, 12.11f, 3.6f, 13.08f, 4.5f, 13.6f)
        verticalLineTo(13.59f)
        curveTo(5f, 13.87f, 5.14f, 14.5f, 4.87f, 14.96f)
        curveTo(4.59f, 15.43f, 4f, 15.6f, 3.5f, 15.32f)
        verticalLineTo(15.33f)
        curveTo(2f, 14.47f, 1f, 12.85f, 1f, 11f)
        arcTo(5f, 5f, 0f, false, true, 6f, 6f)
        curveTo(7f, 3.65f, 9.3f, 2f, 12f, 2f)
        curveTo(15.43f, 2f, 18.24f, 4.66f, 18.5f, 8.03f)
        lineTo(19f, 8f)
        arcTo(4f, 4f, 0f, false, true, 23f, 12f)
        curveTo(23f, 13.5f, 22.2f, 14.77f, 21f, 15.46f)
        verticalLineTo(15.46f)
        curveTo(20.5f, 15.73f, 19.91f, 15.57f, 19.63f, 15.09f)
        curveTo(19.36f, 14.61f, 19.5f, 14f, 20f, 13.72f)
        verticalLineTo(13.73f)
        curveTo(20.6f, 13.39f, 21f, 12.74f, 21f, 12f)
        arcTo(2f, 2f, 0f, false, false, 19f, 10f)
        horizontalLineTo(17f)
        close()
    }
}.build()

/** weather-rainy（来源：Material Design Icons，Apache 2.0 / Pictogrammers Free License）。 */
fun mdiWeatherRainy(tint: Color): ImageVector = ImageVector.Builder(
    name = "weather-rainy",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(tint)) {
        moveTo(6f, 14.03f)
        arcTo(1f, 1f, 0f, false, true, 7f, 15.03f)
        curveTo(7f, 15.58f, 6.55f, 16.03f, 6f, 16.03f)
        curveTo(3.24f, 16.03f, 1f, 13.79f, 1f, 11.03f)
        curveTo(1f, 8.27f, 3.24f, 6.03f, 6f, 6.03f)
        curveTo(7f, 3.68f, 9.3f, 2.03f, 12f, 2.03f)
        curveTo(15.43f, 2.03f, 18.24f, 4.69f, 18.5f, 8.06f)
        lineTo(19f, 8.03f)
        arcTo(4f, 4f, 0f, false, true, 23f, 12.03f)
        curveTo(23f, 14.23f, 21.21f, 16.03f, 19f, 16.03f)
        horizontalLineTo(18f)
        curveTo(17.45f, 16.03f, 17f, 15.58f, 17f, 15.03f)
        curveTo(17f, 14.47f, 17.45f, 14.03f, 18f, 14.03f)
        horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, false, 21f, 12.03f)
        arcTo(2f, 2f, 0f, false, false, 19f, 10.03f)
        horizontalLineTo(17f)
        verticalLineTo(9.03f)
        curveTo(17f, 6.27f, 14.76f, 4.03f, 12f, 4.03f)
        curveTo(9.5f, 4.03f, 7.45f, 5.84f, 7.06f, 8.21f)
        curveTo(6.73f, 8.09f, 6.37f, 8.03f, 6f, 8.03f)
        arcTo(3f, 3f, 0f, false, false, 3f, 11.03f)
        arcTo(3f, 3f, 0f, false, false, 6f, 14.03f)
        moveTo(12f, 14.15f)
        curveTo(12.18f, 14.39f, 12.37f, 14.66f, 12.56f, 14.94f)
        curveTo(13f, 15.56f, 14f, 17.03f, 14f, 18f)
        curveTo(14f, 19.11f, 13.1f, 20f, 12f, 20f)
        arcTo(2f, 2f, 0f, false, true, 10f, 18f)
        curveTo(10f, 17.03f, 11f, 15.56f, 11.44f, 14.94f)
        curveTo(11.63f, 14.66f, 11.82f, 14.4f, 12f, 14.15f)
        moveTo(12f, 11.03f)
        lineTo(11.5f, 11.59f)
        curveTo(11.5f, 11.59f, 10.65f, 12.55f, 9.79f, 13.81f)
        curveTo(8.93f, 15.06f, 8f, 16.56f, 8f, 18f)
        arcTo(4f, 4f, 0f, false, false, 12f, 22f)
        arcTo(4f, 4f, 0f, false, false, 16f, 18f)
        curveTo(16f, 16.56f, 15.07f, 15.06f, 14.21f, 13.81f)
        curveTo(13.35f, 12.55f, 12.5f, 11.59f, 12.5f, 11.59f)
    }
}.build()

/** weather-snowy（来源：Material Design Icons，Apache 2.0 / Pictogrammers Free License）。 */
fun mdiWeatherSnowy(tint: Color): ImageVector = ImageVector.Builder(
    name = "weather-snowy",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(tint)) {
        moveTo(6f, 14f)
        arcTo(1f, 1f, 0f, false, true, 7f, 15f)
        arcTo(1f, 1f, 0f, false, true, 6f, 16f)
        arcTo(5f, 5f, 0f, false, true, 1f, 11f)
        arcTo(5f, 5f, 0f, false, true, 6f, 6f)
        curveTo(7f, 3.65f, 9.3f, 2f, 12f, 2f)
        curveTo(15.43f, 2f, 18.24f, 4.66f, 18.5f, 8.03f)
        lineTo(19f, 8f)
        arcTo(4f, 4f, 0f, false, true, 23f, 12f)
        arcTo(4f, 4f, 0f, false, true, 19f, 16f)
        horizontalLineTo(18f)
        arcTo(1f, 1f, 0f, false, true, 17f, 15f)
        arcTo(1f, 1f, 0f, false, true, 18f, 14f)
        horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, false, 21f, 12f)
        arcTo(2f, 2f, 0f, false, false, 19f, 10f)
        horizontalLineTo(17f)
        verticalLineTo(9f)
        arcTo(5f, 5f, 0f, false, false, 12f, 4f)
        curveTo(9.5f, 4f, 7.45f, 5.82f, 7.06f, 8.19f)
        curveTo(6.73f, 8.07f, 6.37f, 8f, 6f, 8f)
        arcTo(3f, 3f, 0f, false, false, 3f, 11f)
        arcTo(3f, 3f, 0f, false, false, 6f, 14f)
        moveTo(7.88f, 18.07f)
        lineTo(10.07f, 17.5f)
        lineTo(8.46f, 15.88f)
        curveTo(8.07f, 15.5f, 8.07f, 14.86f, 8.46f, 14.46f)
        curveTo(8.85f, 14.07f, 9.5f, 14.07f, 9.88f, 14.46f)
        lineTo(11.5f, 16.07f)
        lineTo(12.07f, 13.88f)
        curveTo(12.21f, 13.34f, 12.76f, 13.03f, 13.29f, 13.17f)
        curveTo(13.83f, 13.31f, 14.14f, 13.86f, 14f, 14.4f)
        lineTo(13.41f, 16.59f)
        lineTo(15.6f, 16f)
        curveTo(16.14f, 15.86f, 16.69f, 16.17f, 16.83f, 16.71f)
        curveTo(16.97f, 17.24f, 16.66f, 17.79f, 16.12f, 17.93f)
        lineTo(13.93f, 18.5f)
        lineTo(15.54f, 20.12f)
        curveTo(15.93f, 20.5f, 15.93f, 21.15f, 15.54f, 21.54f)
        curveTo(15.15f, 21.93f, 14.5f, 21.93f, 14.12f, 21.54f)
        lineTo(12.5f, 19.93f)
        lineTo(11.93f, 22.12f)
        curveTo(11.79f, 22.66f, 11.24f, 22.97f, 10.71f, 22.83f)
        curveTo(10.17f, 22.69f, 9.86f, 22.14f, 10f, 21.6f)
        lineTo(10.59f, 19.41f)
        lineTo(8.4f, 20f)
        curveTo(7.86f, 20.14f, 7.31f, 19.83f, 7.17f, 19.29f)
        curveTo(7.03f, 18.76f, 7.34f, 18.21f, 7.88f, 18.07f)
        close()
    }
}.build()

/** weather-sunny（来源：Material Design Icons，Apache 2.0 / Pictogrammers Free License）。 */
fun mdiWeatherSunny(tint: Color): ImageVector = ImageVector.Builder(
    name = "weather-sunny",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(tint)) {
        moveTo(12f, 7f)
        arcTo(5f, 5f, 0f, false, true, 17f, 12f)
        arcTo(5f, 5f, 0f, false, true, 12f, 17f)
        arcTo(5f, 5f, 0f, false, true, 7f, 12f)
        arcTo(5f, 5f, 0f, false, true, 12f, 7f)
        moveTo(12f, 9f)
        arcTo(3f, 3f, 0f, false, false, 9f, 12f)
        arcTo(3f, 3f, 0f, false, false, 12f, 15f)
        arcTo(3f, 3f, 0f, false, false, 15f, 12f)
        arcTo(3f, 3f, 0f, false, false, 12f, 9f)
        moveTo(12f, 2f)
        lineTo(14.39f, 5.42f)
        curveTo(13.65f, 5.15f, 12.84f, 5f, 12f, 5f)
        curveTo(11.16f, 5f, 10.35f, 5.15f, 9.61f, 5.42f)
        lineTo(12f, 2f)
        moveTo(3.34f, 7f)
        lineTo(7.5f, 6.65f)
        curveTo(6.9f, 7.16f, 6.36f, 7.78f, 5.94f, 8.5f)
        curveTo(5.5f, 9.24f, 5.25f, 10f, 5.11f, 10.79f)
        lineTo(3.34f, 7f)
        moveTo(3.36f, 17f)
        lineTo(5.12f, 13.23f)
        curveTo(5.26f, 14f, 5.53f, 14.78f, 5.95f, 15.5f)
        curveTo(6.37f, 16.24f, 6.91f, 16.86f, 7.5f, 17.37f)
        lineTo(3.36f, 17f)
        moveTo(20.65f, 7f)
        lineTo(18.88f, 10.79f)
        curveTo(18.74f, 10f, 18.47f, 9.23f, 18.05f, 8.5f)
        curveTo(17.63f, 7.78f, 17.1f, 7.15f, 16.5f, 6.64f)
        lineTo(20.65f, 7f)
        moveTo(20.64f, 17f)
        lineTo(16.5f, 17.36f)
        curveTo(17.09f, 16.85f, 17.62f, 16.22f, 18.04f, 15.5f)
        curveTo(18.46f, 14.77f, 18.73f, 14f, 18.87f, 13.21f)
        lineTo(20.64f, 17f)
        moveTo(12f, 22f)
        lineTo(9.59f, 18.56f)
        curveTo(10.33f, 18.83f, 11.14f, 19f, 12f, 19f)
        curveTo(12.82f, 19f, 13.63f, 18.83f, 14.37f, 18.56f)
        lineTo(12f, 22f)
        close()
    }
}.build()

/** weather-windy（来源：Material Design Icons，Apache 2.0 / Pictogrammers Free License）。 */
fun mdiWeatherWindy(tint: Color): ImageVector = ImageVector.Builder(
    name = "weather-windy",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(tint)) {
        moveTo(4f, 10f)
        arcTo(1f, 1f, 0f, false, true, 3f, 9f)
        arcTo(1f, 1f, 0f, false, true, 4f, 8f)
        horizontalLineTo(12f)
        arcTo(2f, 2f, 0f, false, false, 14f, 6f)
        arcTo(2f, 2f, 0f, false, false, 12f, 4f)
        curveTo(11.45f, 4f, 10.95f, 4.22f, 10.59f, 4.59f)
        curveTo(10.2f, 5f, 9.56f, 5f, 9.17f, 4.59f)
        curveTo(8.78f, 4.2f, 8.78f, 3.56f, 9.17f, 3.17f)
        curveTo(9.9f, 2.45f, 10.9f, 2f, 12f, 2f)
        arcTo(4f, 4f, 0f, false, true, 16f, 6f)
        arcTo(4f, 4f, 0f, false, true, 12f, 10f)
        horizontalLineTo(4f)
        moveTo(19f, 12f)
        arcTo(1f, 1f, 0f, false, false, 20f, 11f)
        arcTo(1f, 1f, 0f, false, false, 19f, 10f)
        curveTo(18.72f, 10f, 18.47f, 10.11f, 18.29f, 10.29f)
        curveTo(17.9f, 10.68f, 17.27f, 10.68f, 16.88f, 10.29f)
        curveTo(16.5f, 9.9f, 16.5f, 9.27f, 16.88f, 8.88f)
        curveTo(17.42f, 8.34f, 18.17f, 8f, 19f, 8f)
        arcTo(3f, 3f, 0f, false, true, 22f, 11f)
        arcTo(3f, 3f, 0f, false, true, 19f, 14f)
        horizontalLineTo(5f)
        arcTo(1f, 1f, 0f, false, true, 4f, 13f)
        arcTo(1f, 1f, 0f, false, true, 5f, 12f)
        horizontalLineTo(19f)
        moveTo(18f, 18f)
        horizontalLineTo(4f)
        arcTo(1f, 1f, 0f, false, true, 3f, 17f)
        arcTo(1f, 1f, 0f, false, true, 4f, 16f)
        horizontalLineTo(18f)
        arcTo(3f, 3f, 0f, false, true, 21f, 19f)
        arcTo(3f, 3f, 0f, false, true, 18f, 22f)
        curveTo(17.17f, 22f, 16.42f, 21.66f, 15.88f, 21.12f)
        curveTo(15.5f, 20.73f, 15.5f, 20.1f, 15.88f, 19.71f)
        curveTo(16.27f, 19.32f, 16.9f, 19.32f, 17.29f, 19.71f)
        curveTo(17.47f, 19.89f, 17.72f, 20f, 18f, 20f)
        arcTo(1f, 1f, 0f, false, false, 19f, 19f)
        arcTo(1f, 1f, 0f, false, false, 18f, 18f)
        close()
    }
}.build()

// =====================================================================
// 界面功能图标（非天气图标）
// =====================================================================
// 说明：设置页需要「齿轮」和「关闭」两个图标。它们与天气无关，
// 但同理取自 Material Design Icons（来源/许可同上），
// 由同一脚本 /tmp/svg2compose.py 转换，保证路径与官方 SVG 一致，
// 所以仍放在本文件内，不另开一份许可声明。
// =====================================================================

/** cog（来源：Material Design Icons，Apache 2.0 / Pictogrammers Free License）。 */
fun mdiCog(tint: Color): ImageVector = ImageVector.Builder(
    name = "cog",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(tint)) {
        moveTo(12f, 15.5f)
        arcTo(3.5f, 3.5f, 0f, false, true, 8.5f, 12f)
        arcTo(3.5f, 3.5f, 0f, false, true, 12f, 8.5f)
        arcTo(3.5f, 3.5f, 0f, false, true, 15.5f, 12f)
        arcTo(3.5f, 3.5f, 0f, false, true, 12f, 15.5f)
        moveTo(19.43f, 12.97f)
        curveTo(19.47f, 12.65f, 19.5f, 12.33f, 19.5f, 12f)
        curveTo(19.5f, 11.67f, 19.47f, 11.34f, 19.43f, 11f)
        lineTo(21.54f, 9.37f)
        curveTo(21.73f, 9.22f, 21.78f, 8.95f, 21.66f, 8.73f)
        lineTo(19.66f, 5.27f)
        curveTo(19.54f, 5.05f, 19.27f, 4.96f, 19.05f, 5.05f)
        lineTo(16.56f, 6.05f)
        curveTo(16.04f, 5.66f, 15.5f, 5.32f, 14.87f, 5.07f)
        lineTo(14.5f, 2.42f)
        curveTo(14.46f, 2.18f, 14.25f, 2f, 14f, 2f)
        horizontalLineTo(10f)
        curveTo(9.75f, 2f, 9.54f, 2.18f, 9.5f, 2.42f)
        lineTo(9.13f, 5.07f)
        curveTo(8.5f, 5.32f, 7.96f, 5.66f, 7.44f, 6.05f)
        lineTo(4.95f, 5.05f)
        curveTo(4.73f, 4.96f, 4.46f, 5.05f, 4.34f, 5.27f)
        lineTo(2.34f, 8.73f)
        curveTo(2.21f, 8.95f, 2.27f, 9.22f, 2.46f, 9.37f)
        lineTo(4.57f, 11f)
        curveTo(4.53f, 11.34f, 4.5f, 11.67f, 4.5f, 12f)
        curveTo(4.5f, 12.33f, 4.53f, 12.65f, 4.57f, 12.97f)
        lineTo(2.46f, 14.63f)
        curveTo(2.27f, 14.78f, 2.21f, 15.05f, 2.34f, 15.27f)
        lineTo(4.34f, 18.73f)
        curveTo(4.46f, 18.95f, 4.73f, 19.03f, 4.95f, 18.95f)
        lineTo(7.44f, 17.94f)
        curveTo(7.96f, 18.34f, 8.5f, 18.68f, 9.13f, 18.93f)
        lineTo(9.5f, 21.58f)
        curveTo(9.54f, 21.82f, 9.75f, 22f, 10f, 22f)
        horizontalLineTo(14f)
        curveTo(14.25f, 22f, 14.46f, 21.82f, 14.5f, 21.58f)
        lineTo(14.87f, 18.93f)
        curveTo(15.5f, 18.67f, 16.04f, 18.34f, 16.56f, 17.94f)
        lineTo(19.05f, 18.95f)
        curveTo(19.27f, 19.03f, 19.54f, 18.95f, 19.66f, 18.73f)
        lineTo(21.66f, 15.27f)
        curveTo(21.78f, 15.05f, 21.73f, 14.78f, 21.54f, 14.63f)
        lineTo(19.43f, 12.97f)
        close()
    }
}.build()

/** close（来源：Material Design Icons，Apache 2.0 / Pictogrammers Free License）。 */
fun mdiClose(tint: Color): ImageVector = ImageVector.Builder(
    name = "close",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(tint)) {
        moveTo(19f, 6.41f)
        lineTo(17.59f, 5f)
        lineTo(12f, 10.59f)
        lineTo(6.41f, 5f)
        lineTo(5f, 6.41f)
        lineTo(10.59f, 12f)
        lineTo(5f, 17.59f)
        lineTo(6.41f, 19f)
        lineTo(12f, 13.41f)
        lineTo(17.59f, 19f)
        lineTo(19f, 17.59f)
        lineTo(13.41f, 12f)
        lineTo(19f, 6.41f)
        close()
    }
}.build()

