package com.oopnv70.uilab.ui.weather

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// =====================================================================
// 自绘天气图标集
// =====================================================================
// 为什么不用 Material Icons？
//   1. material-icons-extended 体积巨大（~10MB 依赖，会拖慢构建）；
//   2. 它里面没有「晴 / 多云 / 雷阵雨 / 雪」这类天气专用图形；
//   3. 自绘可以精确控制笔画粗细与配色，风格统一。
//
// 实现方式：用 ImageVector.Builder + path 描述 SVG 路径。
// 每个图标设计尺寸 24x24，与 Material 图标规格一致。
// 所有图标均用「描边（stroke）」绘制，笔帽圆角，视觉更柔和。
// =====================================================================

/** 画笔默认粗细（单位：viewPort 单位，非 dp）。 */
private const val STROKE = 1.9f

/**
 * 构造一个 24x24 的描边图标。
 *
 * @param name 图标标识（用于调试）。
 * @param buildPath 用 [androidx.compose.ui.graphics.vector.PathBuilder] 描述图形。
 * @param tint 描边颜色。这里不写死颜色，由调用方通过 Icon(tint=) 覆盖时用
 *         [Color.Unspecified] 更灵活；为简化，这里统一用传入色。
 */
private fun strokeIcon(
    name: String,
    tint: Color,
    buildPath: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = null,
        stroke = SolidColor(tint),
        strokeLineWidth = STROKE,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = buildPath
    )
}.build()

// ---------------------------------------------------------------------
// 1. 太阳（晴）
// ---------------------------------------------------------------------
/** 太阳本体：圆心 (12,12)，半径 4.2，外加 8 条光芒。 */
private fun androidx.compose.ui.graphics.vector.PathBuilder.sun() {
    // 圆（用两段弧拼成整圆）
    moveTo(12f, 7.8f)
    arcToRelative(4.2f, 4.2f, 0f, true, true, 0f, 8.4f)
    arcToRelative(4.2f, 4.2f, 0f, true, true, 0f, -8.4f)
    close()
    // 八条光芒
    moveTo(12f, 2.2f); lineTo(12f, 4.2f)
    moveTo(12f, 19.8f); lineTo(12f, 21.8f)
    moveTo(2.2f, 12f); lineTo(4.2f, 12f)
    moveTo(19.8f, 12f); lineTo(21.8f, 12f)
    moveTo(5.1f, 5.1f); lineTo(6.5f, 6.5f)
    moveTo(17.5f, 17.5f); lineTo(18.9f, 18.9f)
    moveTo(5.1f, 18.9f); lineTo(6.5f, 17.5f)
    moveTo(17.5f, 6.5f); lineTo(18.9f, 5.1f)
}

fun sunIcon(tint: Color): ImageVector =
    strokeIcon("weather_sun", tint) { sun() }

// ---------------------------------------------------------------------
// 2. 云（阴 / 多云底色）
// ---------------------------------------------------------------------
/** 一朵云：底部一条直线 + 三段圆弧。 */
private fun androidx.compose.ui.graphics.vector.PathBuilder.cloud() {
    moveTo(7f, 18f)
    lineTo(17f, 18f)
    // 右侧大弧
    arcToRelative(4f, 4f, 0f, false, false, 0.4f, -7.98f)
    // 顶部弧
    arcToRelative(5.2f, 5.2f, 0f, false, false, -10.1f, 1.1f)
    // 左侧弧
    arcToRelative(3.6f, 3.6f, 0f, false, false, -0.3f, 6.88f)
    close()
}

fun cloudIcon(tint: Color): ImageVector =
    strokeIcon("weather_cloud", tint) { cloud() }

// ---------------------------------------------------------------------
// 3. 多云（小太阳 + 云）
// ---------------------------------------------------------------------
fun partlyCloudyIcon(tint: Color): ImageVector =
    strokeIcon("weather_partly_cloudy", tint) {
        // 右上角小太阳（半径 3，位于 (16.5, 6.5)）
        moveTo(16.5f, 3.5f)
        arcToRelative(3f, 3f, 0f, true, true, 0f, 6f)
        arcToRelative(3f, 3f, 0f, true, true, 0f, -6f)
        close()
        moveTo(16.5f, 0.6f); lineTo(16.5f, 1.8f)
        moveTo(21.4f, 6.5f); lineTo(22.6f, 6.5f)
        moveTo(20.0f, 3.0f); lineTo(20.9f, 2.1f)
        moveTo(20.0f, 10.0f); lineTo(20.9f, 10.9f)
        // 左下方云
        moveTo(4.5f, 20.5f)
        lineTo(13.5f, 20.5f)
        arcToRelative(3.3f, 3.3f, 0f, false, false, 0.3f, -6.58f)
        arcToRelative(4.3f, 4.3f, 0f, false, false, -8.35f, 0.9f)
        arcToRelative(3f, 3f, 0f, false, false, -0.25f, 5.68f)
        close()
    }

// ---------------------------------------------------------------------
// 4. 雨（云 + 雨滴）
// ---------------------------------------------------------------------
fun rainIcon(tint: Color): ImageVector =
    strokeIcon("weather_rain", tint) {
        // 云（整体上移 2）
        moveTo(7f, 15.5f)
        lineTo(17f, 15.5f)
        arcToRelative(3.8f, 3.8f, 0f, false, false, 0.4f, -7.58f)
        arcToRelative(5f, 5f, 0f, false, false, -9.7f, 1.05f)
        arcToRelative(3.4f, 3.4f, 0f, false, false, -0.3f, 6.53f)
        close()
        // 三滴雨
        moveTo(8.5f, 18.2f); lineTo(7.5f, 21.0f)
        moveTo(12.5f, 18.2f); lineTo(11.5f, 21.0f)
        moveTo(16.5f, 18.2f); lineTo(15.5f, 21.0f)
    }

// ---------------------------------------------------------------------
// 5. 雷阵雨（云 + 闪电）
// ---------------------------------------------------------------------
fun thunderIcon(tint: Color): ImageVector =
    strokeIcon("weather_thunder", tint) {
        moveTo(7f, 15.5f)
        lineTo(17f, 15.5f)
        arcToRelative(3.8f, 3.8f, 0f, false, false, 0.4f, -7.58f)
        arcToRelative(5f, 5f, 0f, false, false, -9.7f, 1.05f)
        arcToRelative(3.4f, 3.4f, 0f, false, false, -0.3f, 6.53f)
        close()
        // 闪电折线
        moveTo(13.2f, 17.0f)
        lineTo(10.6f, 21.2f)
        lineTo(12.4f, 21.2f)
        lineTo(11.2f, 24.0f)
    }

// ---------------------------------------------------------------------
// 6. 雪（云 + 雪花点）
// ---------------------------------------------------------------------
fun snowIcon(tint: Color): ImageVector =
    strokeIcon("weather_snow", tint) {
        moveTo(7f, 15.5f)
        lineTo(17f, 15.5f)
        arcToRelative(3.8f, 3.8f, 0f, false, false, 0.4f, -7.58f)
        arcToRelative(5f, 5f, 0f, false, false, -9.7f, 1.05f)
        arcToRelative(3.4f, 3.4f, 0f, false, false, -0.3f, 6.53f)
        close()
        // 三片雪花（十字）
        moveTo(8.0f, 18.0f); lineTo(8.0f, 21.0f)
        moveTo(6.5f, 19.5f); lineTo(9.5f, 19.5f)
        moveTo(13.0f, 18.0f); lineTo(13.0f, 21.0f)
        moveTo(11.5f, 19.5f); lineTo(14.5f, 19.5f)
    }

// ---------------------------------------------------------------------
// 7. 雾 / 霾（横线）
// ---------------------------------------------------------------------
fun fogIcon(tint: Color): ImageVector =
    strokeIcon("weather_fog", tint) {
        moveTo(3.5f, 8.0f); lineTo(20.5f, 8.0f)
        moveTo(5.5f, 12.0f); lineTo(18.5f, 12.0f)
        moveTo(3.5f, 16.0f); lineTo(20.5f, 16.0f)
        moveTo(6.5f, 20.0f); lineTo(15.5f, 20.0f)
    }

// ---------------------------------------------------------------------
// 8. 微风（风线，用于「风」类指标）
// ---------------------------------------------------------------------
fun windIcon(tint: Color): ImageVector =
    strokeIcon("weather_wind", tint) {
        moveTo(3.0f, 8.0f); lineTo(13.0f, 8.0f)
        arcToRelative(2.6f, 2.6f, 0f, true, false, -2.6f, -2.6f)
        moveTo(3.0f, 16.0f); lineTo(17.0f, 16.0f)
        arcToRelative(2.6f, 2.6f, 0f, true, true, -2.6f, 2.6f)
        moveTo(3.0f, 12.0f); lineTo(20.0f, 12.0f)
    }

// ---------------------------------------------------------------------
// 9. 水滴（湿度）
// ---------------------------------------------------------------------
fun humidityIcon(tint: Color): ImageVector =
    strokeIcon("weather_humidity", tint) {
        // 水滴外形：顶点 + 两侧曲线到圆底
        moveTo(12f, 3.2f)
        curveTo(12f, 3.2f, 5.4f, 11.2f, 5.4f, 15.2f)
        arcToRelative(6.6f, 6.6f, 0f, true, false, 13.2f, 0f)
        curveTo(18.6f, 11.2f, 12f, 3.2f, 12f, 3.2f)
        close()
    }

// ---------------------------------------------------------------------
// 10. 气压表（指针）
// ---------------------------------------------------------------------
fun pressureIcon(tint: Color): ImageVector =
    strokeIcon("weather_pressure", tint) {
        moveTo(4.0f, 17.5f)
        arcToRelative(8f, 8f, 0f, true, true, 16f, 0f)
        close()
        // 指针
        moveTo(12f, 17.5f); lineTo(16.0f, 12.5f)
        // 中心点
        moveTo(11.4f, 17.5f); lineTo(12.6f, 17.5f)
    }

// ---------------------------------------------------------------------
// 11. 日出 / 日落（地平线 + 半日 + 箭头）
// ---------------------------------------------------------------------
fun sunriseIcon(tint: Color): ImageVector =
    strokeIcon("weather_sunrise", tint) {
        moveTo(3.0f, 18.5f); lineTo(21.0f, 18.5f)
        // 半日
        moveTo(7.5f, 18.5f)
        arcToRelative(4.5f, 4.5f, 0f, true, true, 9f, 0f)
        // 光芒
        moveTo(12.0f, 9.0f); lineTo(12.0f, 6.5f)
        moveTo(6.2f, 11.8f); lineTo(4.4f, 10.0f)
        moveTo(17.8f, 11.8f); lineTo(19.6f, 10.0f)
        // 上箭头
        moveTo(12.0f, 21.8f); lineTo(12.0f, 21.0f)
    }

// ---------------------------------------------------------------------
// 12. 定位 / 城市（地图针）
// ---------------------------------------------------------------------
fun locationIcon(tint: Color): ImageVector =
    strokeIcon("weather_location", tint) {
        // 水滴形地图针
        moveTo(12f, 21.5f)
        curveTo(12f, 21.5f, 19f, 14.6f, 19f, 9.6f)
        arcToRelative(7f, 7f, 0f, true, false, -14f, 0f)
        curveTo(5f, 14.6f, 12f, 21.5f, 12f, 21.5f)
        close()
        // 内圆
        moveTo(12f, 7.0f)
        arcToRelative(2.6f, 2.6f, 0f, true, true, 0f, 5.2f)
        arcToRelative(2.6f, 2.6f, 0f, true, true, 0f, -5.2f)
        close()
    }
