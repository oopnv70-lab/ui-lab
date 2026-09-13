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
    /** 是否是「定位到的当前位置」。列表里最多一个。 */
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
     * 根据「当前选中城市」生成对应实况。
     *
     * 逻辑：
     *   - [selected] 为 null → 返回默认（北京）
     *   - 在城市列表里找到同名城市 → 用它的温度/天气构造实况
     *     （这样切换城市时，胶囊和概览页的数字真的会变）
     *   - 定位城市不在预置列表里 → 保留温度，只替换城市名
     *
     * @param selected 当前选中城市；null 表示用默认。
     * @param allCities 候选城市列表（含定位城市）。
     */
    fun currentForCity(
        selected: CityItem?,
        allCities: List<CityItem>
    ): CurrentWeather {
        if (selected == null) return current
        val matched = allCities.firstOrNull { it.name == selected.name }
        return if (matched != null) {
            current.copy(
                city = matched.name,
                temperature = matched.temperature,
                condition = matched.condition,
                // 用温度粗略推体感与高低，保证视觉上自洽
                feelsLike = matched.temperature - 1,
                high = matched.temperature + 3,
                low = matched.temperature - 8,
                summary = "${matched.condition.label} · ${matched.name}"
            )
        } else {
            // 定位城市（不在预置列表）：只换名字
            current.copy(city = selected.name, summary = "${current.condition.label} · ${selected.name}")
        }
    }

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
    /**
     * 可添加的候选城市池（供城市页「添加城市」搜索用）。
     *
     * 与 [cities] 的区别：[cities] 是「已上屏」的默认列表，
     * 这里是「全量可搜索」的池子 —— 用户能从里面挑城市加入列表。
     */
    val allCities: List<CityItem> get() = listOf(
        CityItem("北京", "北京市", 26, SkyCondition.PARTLY_CLOUDY),
        CityItem("上海", "上海市", 24, SkyCondition.RAIN),
        CityItem("广州", "广东省", 31, SkyCondition.THUNDER),
        CityItem("深圳", "广东省", 30, SkyCondition.PARTLY_CLOUDY),
        CityItem("成都", "四川省", 22, SkyCondition.CLOUDY),
        CityItem("杭州", "浙江省", 25, SkyCondition.FOG),
        CityItem("重庆", "重庆市", 29, SkyCondition.CLOUDY),
        CityItem("武汉", "湖北省", 28, SkyCondition.CLEAR),
        CityItem("西安", "陕西省", 21, SkyCondition.FOG),
        CityItem("南京", "江苏省", 26, SkyCondition.PARTLY_CLOUDY),
        CityItem("天津", "天津市", 25, SkyCondition.CLEAR),
        CityItem("苏州", "江苏省", 27, SkyCondition.CLOUDY),
        CityItem("长沙", "湖南省", 30, SkyCondition.THUNDER),
        CityItem("青岛", "山东省", 24, SkyCondition.FOG),
        CityItem("厦门", "福建省", 29, SkyCondition.RAIN),
        CityItem("昆明", "云南省", 20, SkyCondition.CLEAR),
        CityItem("哈尔滨", "黑龙江省", 15, SkyCondition.CLEAR),
        CityItem("沈阳", "辽宁省", 19, SkyCondition.CLOUDY),
        CityItem("郑州", "河南省", 27, SkyCondition.PARTLY_CLOUDY),
        CityItem("拉萨", "西藏自治区", 16, SkyCondition.CLEAR),
        CityItem("乌鲁木齐", "新疆维吾尔自治区", 22, SkyCondition.CLEAR),
        CityItem("香港", "香港特别行政区", 28, SkyCondition.RAIN),
        CityItem("台北", "台湾省", 27, SkyCondition.CLOUDY)
    )

    /** 日出日落（用于概览页底部）。 */
    val sunrise: SourcedValue get() = SourcedValue("05:52", WeatherSource.OPEN_METEO, "今日")
    val sunset: SourcedValue get() = SourcedValue("18:34", WeatherSource.OPEN_METEO, "今日")

    /** 数据更新时间（沿用的旧时间字符串）。 */
    const val UPDATED_AT = "14:15"
}
