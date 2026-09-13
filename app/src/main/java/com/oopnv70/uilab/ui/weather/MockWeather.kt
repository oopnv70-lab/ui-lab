package com.oopnv70.uilab.ui.weather

import androidx.compose.ui.graphics.vector.ImageVector

// =====================================================================
// UI 展示模型（原 MockWeather.kt）
// =====================================================================
// 历史：这个文件曾经放着**手写的假天气数据**（北京 26°、上海 24°…），
// 只为了让界面先"看起来像样"。那是空壳。
//
// 现在：**假数据已全部删除**。本文件只保留 UI 用的类型定义，
// 真实数值来自：
//   data/WeatherApi.kt       → 网络 DTO
//   data/WeatherRepository.kt → 真实请求（Open-Meteo）
//   data/RealWeather.kt      → 真实模型
//   WeatherViewModel.kt      → 状态管理
//   RealWeatherMapper.kt     → 真实数据 → 下面的展示模型
//
// 屏幕上的每一个温度、湿度、风速都来自接口，不再有编造的数字。
// =====================================================================

/** 天气大类：对应底部胶囊导航的四个分组。 */
enum class WeatherGroup(
    val label: String,
    val description: String
) {
    OVERVIEW("概览", "当前天气与关键指标"),
    HOURLY("逐时", "未来 24 小时走势"),
    DAILY("预报", "未来 7 天预报"),
    CITIES("城市", "城市管理与数据来源")
}

/** 数据来源标识（溯源用）。 */
enum class WeatherSource(val displayName: String, val shortName: String) {
    OPEN_METEO("Open-Meteo", "OM"),
    MET_NORWAY("MET Norway", "MET"),
    WTTR_IN("wttr.in", "WTTR")
}

/** 一个带来源标注的数值。 */
data class SourcedValue(
    val value: String,
    val source: WeatherSource,
    /** 观测时刻（本地时间字符串，仅用于展示）。 */
    val observedAt: String
)

/** 天气状况枚举（用于映射图标）。 */
enum class SkyCondition {
    CLEAR,          // 晴
    PARTLY_CLOUDY,  // 多云
    CLOUDY,         // 阴
    RAIN,           // 雨
    THUNDER,        // 雷阵雨
    SNOW,           // 雪
    FOG;            // 雾霾

    /** 映射到对应的自绘图标。 */
    fun icon(tint: androidx.compose.ui.graphics.Color): ImageVector = when (this) {
        CLEAR -> sunIcon(tint)
        PARTLY_CLOUDY -> partlyCloudyIcon(tint)
        CLOUDY -> cloudIcon(tint)
        RAIN -> rainIcon(tint)
        THUNDER -> thunderIcon(tint)
        SNOW -> snowIcon(tint)
        FOG -> fogIcon(tint)
    }

    val label: String
        get() = when (this) {
            CLEAR -> "晴"
            PARTLY_CLOUDY -> "多云"
            CLOUDY -> "阴"
            RAIN -> "小雨"
            THUNDER -> "雷阵雨"
            SNOW -> "小雪"
            FOG -> "雾"
        }
}

/** 当前实况（由真实数据映射而来）。 */
data class CurrentWeather(
    val city: String,
    val condition: SkyCondition,
    val temperature: Int,
    val feelsLike: Int,
    val high: Int,
    val low: Int,
    val summary: String
)

/** 一项关键指标（温度、湿度、风速……）。 */
data class WeatherMetric(
    val label: String,
    val value: String,
    val unit: String,
    val icon: ImageVector,
    val source: WeatherSource,
    /** 附加说明，例如「较昨日 +2°」或风向。 */
    val note: String? = null
)

/** 逐时一项。 */
data class HourlyPoint(
    val time: String,
    val temperature: Int,
    val condition: SkyCondition,
    val precipitation: Int,   // 降水概率 %
    val isNow: Boolean = false
)

/** 每日一项。 */
data class DailyPoint(
    val weekday: String,
    val date: String,
    val condition: SkyCondition,
    val high: Int,
    val low: Int,
    val precipitation: Int
)

/** 城市一项。 */
data class CityItem(
    val name: String,
    val admin: String,
    val temperature: Int,
    val condition: SkyCondition,
    /** 是否是「定位到的当前位置」。列表里最多一个。 */
    val isCurrent: Boolean = false
)