package com.oopnv70.uilab.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =====================================================================
// Open-Meteo 网络层 DTO
// =====================================================================
// 用两个免 Key 的官方接口：
//
//   1) Geocoding（按地名搜城市，返回经纬度）
//      https://geocoding-api.open-meteo.com/v1/search?name=深圳&count=10&language=zh&format=json
//
//   2) Forecast（按经纬度取天气）
//      https://api.open-meteo.com/v1/forecast
//        ?latitude=22.55&longitude=114.07
//        &current=temperature_2m,relative_humidity_2m,apparent_temperature,
//                 is_day,precipitation,weather_code,wind_speed_10m,
//                 surface_pressure
//        &hourly=temperature_2m,precipitation_probability,weather_code
//        &daily=weather_code,temperature_2m_max,temperature_2m_min,
//               precipitation_probability_max,sunrise,sunset,uv_index_max
//        &timezone=auto
//
// 两者都返回 JSON，无需 API Key，且允许商用（CC BY 4.0，需署名）。
// =====================================================================

// ------------------------- Geocoding -------------------------

/** 地理编码搜索响应。 */
@Serializable
data class GeoSearchResponse(
    /** 匹配到的地点列表；没有匹配时为 null。 */
    @SerialName("results") val results: List<GeoPlaceDto>? = null
)

/** 一个匹配到的地点。 */
@Serializable
data class GeoPlaceDto(
    @SerialName("id") val id: Long = 0,
    /** 地名（如「深圳」）。 */
    @SerialName("name") val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    /** 国家（中文时如「中国」）。 */
    @SerialName("country") val country: String? = null,
    /** 一级行政区（中文时如「广东省」）。 */
    @SerialName("admin1") val admin1: String? = null,
    /** 二级行政区（如「深圳市」）。 */
    @SerialName("admin2") val admin2: String? = null,
    /** 时区。 */
    @SerialName("timezone") val timezone: String? = null,
    /** 人口（可用于排序/去重）。 */
    @SerialName("population") val population: Long? = null,
    /** 地物类型：PPL=城镇，PPLC=首都，ADM1=一级行政区… */
    @SerialName("feature_code") val featureCode: String? = null
) {
    /**
     * 组装给 UI 显示的「省份 · 国家」副标题。
     * 中文接口下 admin1 是「广东省」，country 是「中国」。
     */
    val subtitle: String
        get() = listOfNotNull(
            admin1?.takeIf { it.isNotBlank() && it != name },
            country?.takeIf { it.isNotBlank() }
        ).distinct().joinToString(" · ")
}

// ------------------------- Forecast -------------------------

/** 天气接口响应。 */
@Serializable
data class ForecastResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String? = null,
    /** 当前实况（请求里带 current= 才会有）。 */
    @SerialName("current") val current: CurrentDto? = null,
    @SerialName("hourly") val hourly: HourlyDto? = null,
    @SerialName("daily") val daily: DailyDto? = null
)

@Serializable
data class CurrentDto(
    @SerialName("time") val time: String? = null,
    /** 气温 °C。 */
    @SerialName("temperature_2m") val temperature: Double? = null,
    /** 相对湿度 %。 */
    @SerialName("relative_humidity_2m") val humidity: Double? = null,
    /** 体感温度 °C。 */
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    /** 是否白天（1/0）。 */
    @SerialName("is_day") val isDay: Int? = null,
    /** 降水量 mm。 */
    @SerialName("precipitation") val precipitation: Double? = null,
    /** WMO 天气码。 */
    @SerialName("weather_code") val weatherCode: Int? = null,
    /** 风速 km/h（默认单位）。 */
    @SerialName("wind_speed_10m") val windSpeed: Double? = null,
    /** 风向 °。 */
    @SerialName("wind_direction_10m") val windDirection: Double? = null,
    /** 地面气压 hPa。 */
    @SerialName("surface_pressure") val pressure: Double? = null
)

@Serializable
data class HourlyDto(
    @SerialName("time") val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList()
)

@Serializable
data class DailyDto(
    @SerialName("time") val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?> = emptyList(),
    /** 日出（ISO8601，如 2026-09-13T05:52）。 */
    @SerialName("sunrise") val sunrise: List<String> = emptyList(),
    /** 日落。 */
    @SerialName("sunset") val sunset: List<String> = emptyList(),
    /** 紫外线指数最大值。 */
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList()
)