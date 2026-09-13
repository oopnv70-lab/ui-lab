package com.oopnv70.uilab.data

// =====================================================================
// WMO 天气码 → 中文描述
// =====================================================================
// Open-Meteo 用 WMO Weather Interpretation Codes（WW）。
// 官方对照表：https://open-meteo.com/en/docs（WMO Weather interpretation codes）
//
// 完整码表（0~99）在这里全部覆盖，未知码回退到「未知」而不是崩。
// =====================================================================

/** 天气码的中文描述 + 归类（归类用于挑图标）。 */
enum class WeatherKind {
    CLEAR,          // 晴
    MAINLY_CLEAR,   // 多云（晴间多云）
    CLOUDY,         // 阴
    FOG,            // 雾
    DRIZZLE,        // 毛毛雨
    RAIN,           // 雨
    FREEZING,       // 冻雨
    SNOW,           // 雪
    SHOWER,         // 阵雨
    SNOW_SHOWER,    // 阵雪
    THUNDER;        // 雷暴

    /** 归类对应的中文短标签（供城市列表等紧凑场景用）。 */
    val shortLabel: String
        get() = when (this) {
            CLEAR -> "晴"
            MAINLY_CLEAR -> "多云"
            CLOUDY -> "阴"
            FOG -> "雾"
            DRIZZLE -> "毛毛雨"
            RAIN -> "雨"
            FREEZING -> "冻雨"
            SNOW -> "雪"
            SHOWER -> "阵雨"
            SNOW_SHOWER -> "阵雪"
            THUNDER -> "雷阵雨"
        }
}

/**
 * WMO 天气码的完整中文描述。
 *
 * 例如 61 -> "小雨"，95 -> "雷阵雨"。
 * 未知码返回 "未知天气（码 N）"，便于排查。
 */
fun wmoDescription(code: Int?): String = when (code) {
    0 -> "晴"
    1 -> "晴间多云"
    2 -> "多云"
    3 -> "阴"
    45 -> "有雾"
    48 -> "雾凇"
    51 -> "小毛毛雨"
    53 -> "毛毛雨"
    55 -> "大毛毛雨"
    56 -> "轻度冻毛毛雨"
    57 -> "强冻毛毛雨"
    61 -> "小雨"
    63 -> "中雨"
    65 -> "大雨"
    66 -> "轻度冻雨"
    67 -> "强冻雨"
    71 -> "小雪"
    73 -> "中雪"
    75 -> "大雪"
    77 -> "雪粒"
    80 -> "小阵雨"
    81 -> "中阵雨"
    82 -> "强阵雨"
    85 -> "小阵雪"
    86 -> "大阵雪"
    95 -> "雷阵雨"
    96 -> "雷阵雨伴小冰雹"
    99 -> "雷阵雨伴大冰雹"
    null -> "暂无数据"
    else -> "未知天气（码 $code）"
}

/** 天气码 → 归类（用于挑图标）。 */
fun wmoKind(code: Int?): WeatherKind = when (code) {
    0 -> WeatherKind.CLEAR
    1, 2 -> WeatherKind.MAINLY_CLEAR
    3 -> WeatherKind.CLOUDY
    45, 48 -> WeatherKind.FOG
    51, 53, 55 -> WeatherKind.DRIZZLE
    56, 57, 66, 67 -> WeatherKind.FREEZING
    61, 63, 65 -> WeatherKind.RAIN
    71, 73, 75, 77 -> WeatherKind.SNOW
    80, 81, 82 -> WeatherKind.SHOWER
    85, 86 -> WeatherKind.SNOW_SHOWER
    95, 96, 99 -> WeatherKind.THUNDER
    else -> WeatherKind.CLOUDY
}

/** 风向角度 → 中文方位（8 方位）。 */
fun windDirectionText(degrees: Double?): String {
    if (degrees == null) return ""
    val dirs = listOf("北", "东北", "东", "东南", "南", "西南", "西", "西北")
    val idx = (((degrees % 360) + 360) % 360 / 45.0).toInt() % 8
    return "${dirs[idx]}风"
}

/**
 * 蒲福风级（0~12），按 km/h 换算。
 * 用于把风速变成「2 级」这种更好懂的说法。
 */
fun windLevel(kmh: Double?): Int {
    if (kmh == null) return 0
    val thresholds = listOf(1.0, 5.0, 11.0, 19.0, 28.0, 38.0, 49.0, 61.0, 74.0, 88.0, 102.0, 117.0)
    return thresholds.indexOfFirst { kmh < it }.let { if (it == -1) 12 else it }
}

/** 紫外线指数 → 中文强度说明。 */
fun uvLevelText(uv: Double?): String = when {
    uv == null -> ""
    uv < 3 -> "较弱，无需特别防护"
    uv < 6 -> "中等，建议防晒"
    uv < 8 -> "较强，注意防晒"
    uv < 11 -> "很强，务必防晒"
    else -> "极强，尽量避免外出"
}

/** 湿度 → 简短体感说明。 */
fun humidityText(h: Double?): String = when {
    h == null -> ""
    h < 30 -> "空气偏干"
    h < 60 -> "湿度舒适"
    h < 80 -> "较为潮湿"
    else -> "非常潮湿"
}