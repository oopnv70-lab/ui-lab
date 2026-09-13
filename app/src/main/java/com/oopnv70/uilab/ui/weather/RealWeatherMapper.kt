package com.oopnv70.uilab.ui.weather

import androidx.compose.ui.graphics.Color
import com.oopnv70.uilab.data.RealDaily
import com.oopnv70.uilab.data.RealHourly
import com.oopnv70.uilab.data.RealWeather
import com.oopnv70.uilab.data.WeatherKind
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// =====================================================================
// 真实数据 → UI 模型 的映射
// =====================================================================
// UI 层（WeatherPages.kt）渲染的是 CurrentWeather / HourlyPoint / DailyPoint
// 这些「展示模型」。本文件负责把 data 层的 RealWeather 转成它们。
//
// 好处：UI 一行都不用改，而屏幕上的每个数字都来自真实接口。
// =====================================================================

/** data 层的 WeatherKind → UI 层的 SkyCondition（图标/文案映射）。 */
fun WeatherKind.toSkyCondition(): SkyCondition = when (this) {
    WeatherKind.CLEAR -> SkyCondition.CLEAR
    WeatherKind.MAINLY_CLEAR -> SkyCondition.PARTLY_CLOUDY
    WeatherKind.CLOUDY -> SkyCondition.CLOUDY
    WeatherKind.FOG -> SkyCondition.FOG
    WeatherKind.DRIZZLE -> SkyCondition.RAIN
    WeatherKind.RAIN -> SkyCondition.RAIN
    WeatherKind.FREEZING -> SkyCondition.SNOW
    WeatherKind.SNOW -> SkyCondition.SNOW
    WeatherKind.SNOW_SHOWER -> SkyCondition.SNOW
    WeatherKind.SHOWER -> SkyCondition.RAIN
    WeatherKind.THUNDER -> SkyCondition.THUNDER
}

/**
 * 真实天气 → 概览页的 [CurrentWeather]。
 *
 * 注意 [CurrentWeather.condition] 只用于「挑图标」，
 * 文字一律用 [RealWeather.conditionText]（WMO 码的精确中文描述，
 * 比枚举的短标签更准，例如「小毛毛雨」「雷阵雨伴小冰雹」）。
 */
fun RealWeather.toCurrentWeather(): CurrentWeather = CurrentWeather(
    city = "",
    condition = kind.toSkyCondition(),
    temperature = temperature,
    feelsLike = feelsLike,
    high = high,
    low = low,
    summary = buildSummary()
)

/** 用真实数据拼一句摘要，例如「多云 · 体感 24° · 降水概率 10%」。 */
private fun RealWeather.buildSummary(): String = buildList {
    add(conditionText)
    if (temperatureRaw != null) add("体感 ${feelsLike}°")
    precipitationProbability?.let { add("降水概率 $it%") }
    if (humidity != null) add("湿度 $humidity%")
}.joinToString(" · ")

/** 真实天气 → 逐时页的 [HourlyPoint] 列表。 */
fun RealWeather.toHourlyPoints(): List<HourlyPoint> =
    hourly.map { h -> h.toHourlyPoint() }

private fun RealHourly.toHourlyPoint(): HourlyPoint = HourlyPoint(
    time = time,
    temperature = temperature,
    condition = kind.toSkyCondition(),
    precipitation = precipitation ?: 0,
    isNow = isNow
)

/** 真实天气 → 预报页的 [DailyPoint] 列表。 */
fun RealWeather.toDailyPoints(): List<DailyPoint> =
    daily.map { d -> d.toDailyPoint() }

private fun RealDaily.toDailyPoint(): DailyPoint = DailyPoint(
    weekday = weekday,
    date = date,
    condition = kind.toSkyCondition(),
    high = high,
    low = low,
    precipitation = precipitation ?: 0
)

/**
 * 真实天气 → 关键指标列表（概览页的六宫格）。
 *
 * 每一项都标注来源：Open-Meteo。没拿到的字段显示「—」，不编数字。
 */
fun RealWeather.toMetrics(): List<WeatherMetric> {
    val tint = Color.Unspecified
    return listOf(
        WeatherMetric(
            label = "体感温度",
            value = feelsLike.toString(),
            unit = "°",
            icon = humidityIcon(tint),
            source = WeatherSource.OPEN_METEO,
            note = if (temperatureRaw != null) {
                val d = feelsLike - temperature
                when {
                    d > 0 -> "比实际高 ${d}°"
                    d < 0 -> "比实际低 ${-d}°"
                    else -> "与实际一致"
                }
            } else null
        ),
        WeatherMetric(
            label = "湿度",
            value = humidity?.toString() ?: "—",
            unit = if (humidity != null) "%" else "",
            icon = humidityIcon(tint),
            source = WeatherSource.OPEN_METEO,
            note = humidityNote.ifBlank { null }
        ),
        WeatherMetric(
            label = "风速",
            value = windSpeed?.let { "%.1f".format(it) } ?: "—",
            unit = if (windSpeed != null) "km/h" else "",
            icon = windIcon(tint),
            source = WeatherSource.OPEN_METEO,
            note = if (windText.isBlank() && windSpeed == null) null
            else listOf(windText, "${windLevel} 级").filter { it.isNotBlank() }.joinToString(" ")
        ),
        WeatherMetric(
            label = "气压",
            value = pressure?.toString() ?: "—",
            unit = if (pressure != null) "hPa" else "",
            icon = pressureIcon(tint),
            source = WeatherSource.OPEN_METEO,
            note = null
        ),
        WeatherMetric(
            label = "紫外线",
            value = uvIndex?.let { "%.1f".format(it) } ?: "—",
            unit = if (uvIndex != null) "级" else "",
            icon = sunIcon(tint),
            source = WeatherSource.OPEN_METEO,
            note = uvText.ifBlank { null }
        ),
        WeatherMetric(
            label = "降水概率",
            value = precipitationProbability?.toString() ?: "—",
            unit = if (precipitationProbability != null) "%" else "",
            icon = rainIcon(tint),
            source = WeatherSource.OPEN_METEO,
            note = when {
                precipitationProbability == null -> null
                precipitationProbability == 0 -> "今天无降水"
                precipitationProbability < 40 -> "降水可能性较小"
                precipitationProbability < 70 -> "可能有降水"
                else -> "降水可能性很大"
            }
        )
    )
}

/** 更新时间显示（从接口给的 observedAt 里取 HH:mm）。 */
fun RealWeather.observedTimeText(): String {
    val raw = observedAt
    if (raw.isBlank()) return ""
    return raw.substringAfter("T").take(5).ifBlank {
        runCatching {
            LocalDateTime.parse(raw).format(DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrDefault("")
    }
}