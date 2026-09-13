package com.oopnv70.uilab.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =====================================================================
// MET Norway（挪威气象局）网络层 DTO
// =====================================================================
// 接口：
//   https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=..&lon=..
//
// 特点（与 Open-Meteo 的差异，接入时必须注意）：
//   - **免 Key**，但官方要求带一个能联系到开发者的 User-Agent。
//   - 时间一律是 **UTC**（形如 2026-09-13T18:00:00Z），展示前要换算。
//   - 风速单位是 **m/s**，而 Open-Meteo 默认是 km/h，融合时不能直接比。
//   - 天气现象用 **symbol_code**（clearsky_day / partlycloudy_night …），
//     不是 WMO 数字码，需要单独映射。
//   - properties.meta.updated_at 是**数据更新时间**，可用来判断「谁最新」。
//
// 已实测确认结构：properties.timeseries[i].data 里
//   instant.details      → 温度/气压/湿度/云量/风向风速
//   next_1_hours.summary → symbol_code，details.precipitation_amount → 降水量
//   next_6_hours / next_12_hours → 同上（用于更长的时段）
// =====================================================================

/** 顶层响应：GeoJSON Feature。 */
@Serializable
data class MetNoResponse(
    @SerialName("geometry") val geometry: MetNoGeometry? = null,
    @SerialName("properties") val properties: MetNoProperties? = null
)

@Serializable
data class MetNoGeometry(
    /** [经度, 纬度, 海拔]。注意 GeoJSON 是「经在前」。 */
    @SerialName("coordinates") val coordinates: List<Double> = emptyList()
) {
    val longitude: Double? get() = coordinates.getOrNull(0)
    val latitude: Double? get() = coordinates.getOrNull(1)
}

@Serializable
data class MetNoProperties(
    @SerialName("meta") val meta: MetNoMeta? = null,
    @SerialName("timeseries") val timeseries: List<MetNoTimeseries> = emptyList()
)

@Serializable
data class MetNoMeta(
    /** 数据更新时间（UTC ISO8601），用于新鲜度比较。 */
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class MetNoTimeseries(
    /** 该条数据对应的时刻（UTC ISO8601）。 */
    @SerialName("time") val time: String = "",
    @SerialName("data") val data: MetNoData? = null
)

@Serializable
data class MetNoData(
    @SerialName("instant") val instant: MetNoInstant? = null,
    @SerialName("next_1_hours") val next1Hours: MetNoPeriod? = null,
    @SerialName("next_6_hours") val next6Hours: MetNoPeriod? = null,
    @SerialName("next_12_hours") val next12Hours: MetNoPeriod? = null
)

@Serializable
data class MetNoInstant(
    @SerialName("details") val details: MetNoInstantDetails? = null
)

@Serializable
data class MetNoInstantDetails(
    /** 海平面气压 hPa。 */
    @SerialName("air_pressure_at_sea_level") val pressure: Double? = null,
    /** 气温 °C。 */
    @SerialName("air_temperature") val temperature: Double? = null,
    /** 云量 %。 */
    @SerialName("cloud_area_fraction") val cloudAreaFraction: Double? = null,
    /** 相对湿度 %。 */
    @SerialName("relative_humidity") val humidity: Double? = null,
    /** 风向（度，0=北）。 */
    @SerialName("wind_from_direction") val windFromDirection: Double? = null,
    /** 风速 **m/s**。 */
    @SerialName("wind_speed") val windSpeed: Double? = null
)

/** 某个未来时段（1/6/12 小时）的预报。 */
@Serializable
data class MetNoPeriod(
    @SerialName("summary") val summary: MetNoSummary? = null,
    @SerialName("details") val details: MetNoPeriodDetails? = null
)

@Serializable
data class MetNoSummary(
    /** 天气现象代号，如 clearsky_day、partlycloudy_night、rain。 */
    @SerialName("symbol_code") val symbolCode: String? = null
)

@Serializable
data class MetNoPeriodDetails(
    /** 该时段累计降水量 mm。 */
    @SerialName("precipitation_amount") val precipitationAmount: Double? = null
)
