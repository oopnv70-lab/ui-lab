package com.oopnv70.uilab.ui.weather

import androidx.compose.ui.graphics.vector.ImageVector

// =====================================================================
// 演示用天气数据（Mock）
// =====================================================================
// 目的：先把 UI 的「样子」做出来给用户看，数据层对接前用这些占位。
// 数据是手工编的，但刻意做得「像真的」：
//   - 温度曲线符合一天的自然走势（凌晨最低、午后最高）
//   - 降水概率和天气类型自洽（晴天降水 0%，下雨时才有值）
//   - 数据来源标注（provenance）也一并模拟，呼应「小字标注来源」需求
// 接入真实数据后，本文件可整体删除。
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
            FOG -> "霾"
        }
}

/** 当前实况。 */
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
    val isCurrent: Boolean = false
)

// ---------------------------------------------------------------------
// 假数据构造
// ---------------------------------------------------------------------

object MockWeather {
    val current = CurrentWeather(
        city = "北京",
        condition = SkyCondition.PARTLY_CLOUDY,
        temperature = 26,
        feelsLike = 24,
        high = 29,
        low = 18,
        summary = "多云转晴 · 午后体感舒适"
    )

    /**
     * 把 [cityName] 覆盖到实况数据上。
     *
     * 用途：定位成功后，把写死的「北京」替换成真实城市名。
     * 天气数字暂时仍是假数据 —— 等数据层落地后这里会换成真实源。
     *
     * @param cityName 真实城市显示名（如「广东省深圳市」）；null 时原样返回。
     */
    fun currentFor(cityName: String?): CurrentWeather =
        if (cityName.isNullOrBlank()) current else current.copy(city = cityName)

    /**
     * 城市列表，「当前位置」那一项用真实定位结果替换。
     *
     * @param currentCityName 真实城市名；null 时保留原来的占位「北京」。
     */
    fun citiesWith(currentCityName: String?): List<CityItem> {
        val base = cities
        if (currentCityName.isNullOrBlank()) return base
        return base.map { item ->
            if (item.isCurrent) {
                item.copy(
                    name = currentCityName,
                    admin = "",
                    isCurrent = true
                )
            } else {
                item
            }
        }
    }


    val metrics: List<WeatherMetric> get() = listOf(
        WeatherMetric(
            label = "体感温度",
            value = "24",
            unit = "°",
            icon = humidityIcon(androidx.compose.ui.graphics.Color.Unspecified),
            source = WeatherSource.OPEN_METEO,
            note = "比实际低 2°"
        ),
        WeatherMetric(
            label = "湿度",
            value = "19",
            unit = "%",
            icon = humidityIcon(androidx.compose.ui.graphics.Color.Unspecified),
            source = WeatherSource.MET_NORWAY,
            note = "空气偏干"
        ),
        WeatherMetric(
            label = "风速",
            value = "9.1",
            unit = "km/h",
            icon = windIcon(androidx.compose.ui.graphics.Color.Unspecified),
            source = WeatherSource.OPEN_METEO,
            note = "东南风 2 级"
        ),
        WeatherMetric(
            label = "气压",
            value = "1021",
            unit = "hPa",
            icon = pressureIcon(androidx.compose.ui.graphics.Color.Unspecified),
            source = WeatherSource.MET_NORWAY,
            note = "高于常年"
        ),
        WeatherMetric(
            label = "紫外线",
            value = "6",
            unit = "级",
            icon = sunIcon(androidx.compose.ui.graphics.Color.Unspecified),
            source = WeatherSource.OPEN_METEO,
            note = "较强，注意防晒"
        ),
        WeatherMetric(
            label = "能见度",
            value = "10",
            unit = "km",
            icon = fogIcon(androidx.compose.ui.graphics.Color.Unspecified),
            source = WeatherSource.WTTR_IN,
            note = "视野通透"
        )
    )

    /** 未来 24 小时（从「现在」开始）。 */
    val hourly: List<HourlyPoint> get() {
        val nowHour = 14
        val temps = listOf(
            26, 27, 28, 29, 28, 26, 24, 22, 21, 20, 19, 19,
            18, 18, 18, 19, 21, 23, 25, 27, 28, 28, 27, 26
        )
        val conds = listOf(
            SkyCondition.PARTLY_CLOUDY, SkyCondition.PARTLY_CLOUDY, SkyCondition.CLEAR, SkyCondition.CLEAR,
            SkyCondition.CLEAR, SkyCondition.PARTLY_CLOUDY, SkyCondition.PARTLY_CLOUDY, SkyCondition.CLOUDY,
            SkyCondition.CLOUDY, SkyCondition.CLOUDY, SkyCondition.CLOUDY, SkyCondition.CLOUDY,
            SkyCondition.CLOUDY, SkyCondition.CLOUDY, SkyCondition.PARTLY_CLOUDY, SkyCondition.PARTLY_CLOUDY,
            SkyCondition.CLEAR, SkyCondition.CLEAR, SkyCondition.CLEAR, SkyCondition.PARTLY_CLOUDY,
            SkyCondition.PARTLY_CLOUDY, SkyCondition.PARTLY_CLOUDY, SkyCondition.PARTLY_CLOUDY, SkyCondition.CLOUDY
        )
        val precip = listOf(
            0, 0, 0, 0, 5, 10, 10, 15, 20, 25, 25, 30,
            30, 25, 20, 15, 10, 5, 0, 0, 0, 5, 10, 10
        )
        return temps.indices.map { i ->
            val h = (nowHour + i) % 24
            HourlyPoint(
                time = if (i == 0) "现在" else "%02d:00".format(h),
                temperature = temps[i],
                condition = conds[i],
                precipitation = precip[i],
                isNow = i == 0
            )
        }
    }

    /** 未来 7 天。 */
    val daily: List<DailyPoint> get() = listOf(
        DailyPoint("今天", "9/13", SkyCondition.PARTLY_CLOUDY, 29, 18, 10),
        DailyPoint("周日", "9/14", SkyCondition.CLEAR, 31, 19, 0),
        DailyPoint("周一", "9/15", SkyCondition.CLOUDY, 27, 17, 30),
        DailyPoint("周二", "9/16", SkyCondition.RAIN, 23, 16, 80),
        DailyPoint("周三", "9/17", SkyCondition.THUNDER, 22, 15, 90),
        DailyPoint("周四", "9/18", SkyCondition.RAIN, 24, 16, 60),
        DailyPoint("周五", "9/19", SkyCondition.PARTLY_CLOUDY, 28, 18, 20)
    )

    /** 城市列表（含当前定位城市）。 */
    val cities: List<CityItem> get() = listOf(
        CityItem("北京", "北京市", 26, SkyCondition.PARTLY_CLOUDY, isCurrent = true),
        CityItem("上海", "上海市", 24, SkyCondition.RAIN, isCurrent = false),
        CityItem("广州", "广东省", 31, SkyCondition.THUNDER, isCurrent = false),
        CityItem("深圳", "广东省", 30, SkyCondition.PARTLY_CLOUDY, isCurrent = false),
        CityItem("成都", "四川省", 22, SkyCondition.CLOUDY, isCurrent = false),
        CityItem("杭州", "浙江省", 25, SkyCondition.FOG, isCurrent = false)
    )

    /** 日出日落（用于概览页底部）。 */
    val sunrise: SourcedValue get() = SourcedValue("05:52", WeatherSource.OPEN_METEO, "今日")
    val sunset: SourcedValue get() = SourcedValue("18:34", WeatherSource.OPEN_METEO, "今日")

    /** 数据更新时间（沿用的旧时间字符串）。 */
    const val UPDATED_AT = "14:15"
}
