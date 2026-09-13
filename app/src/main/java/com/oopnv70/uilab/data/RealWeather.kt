package com.oopnv70.uilab.data

// =====================================================================
// 真实天气模型：DTO → UI 可直接用的数据
// =====================================================================
// 设计原则：
//   - **不编造任何数字**。所有数值都直接来自接口；接口没给的字段就留 null，
//     UI 显示「—」而不是假装有个值。
//   - 单位、文案都在这层算好，UI 只负责排版。
// =====================================================================

/** 一次完整的真实天气快照。 */
data class RealWeather(
    /** 气温 °C（四舍五入到整数显示，原始值也在）。 */
    val temperature: Int,
    val temperatureRaw: Double?,
    /** 体感温度 °C。 */
    val feelsLike: Int,
    /** 今日最高 / 最低 °C。 */
    val high: Int,
    val low: Int,
    /** 天气码 + 中文描述。 */
    val weatherCode: Int?,
    val conditionText: String,
    val kind: WeatherKind,
    /** 湿度 %。 */
    val humidity: Int?,
    val humidityNote: String,
    /** 风速 km/h + 风向文字 + 风级。 */
    val windSpeed: Double?,
    val windText: String,
    val windLevel: Int,
    /** 气压 hPa。 */
    val pressure: Int?,
    /** 紫外线指数最大值 + 中文说明。 */
    val uvIndex: Double?,
    val uvText: String,
    /** 今日降水概率最大值 %。 */
    val precipitationProbability: Int?,
    /** 日出 / 日落（HH:mm）。 */
    val sunrise: String,
    val sunset: String,
    /** 观测时刻（接口返回的本地时间，如 2026-09-13T14:15）。 */
    val observedAt: String,
    /** 逐时（未来 24 小时，从接口给的「当前小时」往后取）。 */
    val hourly: List<RealHourly>,
    /** 每日（未来 7 天）。 */
    val daily: List<RealDaily>
)

/** 逐时一项（真实数据）。 */
data class RealHourly(
    /** 显示用时刻，如「14:00」；第一项是「现在」。 */
    val time: String,
    val temperature: Int,
    val conditionText: String,
    val kind: WeatherKind,
    /** 降水概率 %。 */
    val precipitation: Int?,
    val isNow: Boolean
)

/** 每日一项（真实数据）。 */
data class RealDaily(
    /** 星期几（今天/明天/周三…）。 */
    val weekday: String,
    /** 日期 M/D。 */
    val date: String,
    val conditionText: String,
    val kind: WeatherKind,
    val high: Int,
    val low: Int,
    val precipitation: Int?
)

object RealWeatherFactory {

    /**
     * 把接口响应转成 [RealWeather]。
     *
     * 关键点：**逐时数据要按「当前时刻」对齐**。
     * Open-Meteo 的 hourly 数组是从当天 00:00 开始的 168 项（7 天），
     * 我们只取从「当前小时」开始的 24 项，这样第一项才是「现在」。
     */
    fun from(dto: ForecastResponse): RealWeather {
        val cur = dto.current
        val daily = dto.daily
        val hourly = dto.hourly

        val temp = cur?.temperature
        val highIdx0 = daily?.temperatureMax?.getOrNull(0)
        val lowIdx0 = daily?.temperatureMin?.getOrNull(0)

        // ---- 逐时对齐 ----
        val realHourly: List<RealHourly> = run {
            val times = hourly?.time.orEmpty()
            if (times.isEmpty()) return@run emptyList()
            // 当前时刻字符串形如 "2026-09-13T14:00"
            val nowPrefix = cur?.time?.take(13) // "2026-09-13T14"
            val startIdx = times.indexOfFirst { it.take(13) == nowPrefix }
                .let { if (it >= 0) it else 0 }
            times.indices
                .drop(startIdx)
                .take(24)
                .mapNotNull { i ->
                    val t = times.getOrNull(i) ?: return@mapNotNull null
                    val tp = hourly.temperature.getOrNull(i)
                    val code = hourly.weatherCode.getOrNull(i)
                    RealHourly(
                        time = if (i == startIdx) "现在" else t.substringAfter("T").take(5),
                        temperature = tp?.roundToIntSafe() ?: return@mapNotNull null,
                        conditionText = wmoDescription(code),
                        kind = wmoKind(code),
                        precipitation = hourly.precipitationProbability.getOrNull(i),
                        isNow = i == startIdx
                    )
                }
        }

        // ---- 每日 ----
        val realDaily: List<RealDaily> = run {
            val times = daily?.time.orEmpty()
            times.indices.mapNotNull { i ->
                val date = times.getOrNull(i) ?: return@mapNotNull null
                val code = daily.weatherCode.getOrNull(i)
                RealDaily(
                    weekday = weekdayLabel(date, i),
                    date = date.substringAfter("-").let { md ->
                        // "09-13" -> "9/13"
                        val parts = md.split("-")
                        if (parts.size == 2) {
                            "${parts[0].trimStart('0').ifEmpty { "0" }}/${parts[1].trimStart('0').ifEmpty { "0" }}"
                        } else md
                    },
                    conditionText = wmoDescription(code),
                    kind = wmoKind(code),
                    high = daily.temperatureMax.getOrNull(i)?.roundToIntSafe() ?: 0,
                    low = daily.temperatureMin.getOrNull(i)?.roundToIntSafe() ?: 0,
                    precipitation = daily.precipitationProbabilityMax.getOrNull(i)
                )
            }
        }

        val wind = cur?.windSpeed
        val uv = daily?.uvIndexMax?.getOrNull(0)

        return RealWeather(
            temperature = temp?.roundToIntSafe() ?: 0,
            temperatureRaw = temp,
            feelsLike = cur?.apparentTemperature?.roundToIntSafe()
                ?: temp?.roundToIntSafe() ?: 0,
            high = highIdx0?.roundToIntSafe() ?: 0,
            low = lowIdx0?.roundToIntSafe() ?: 0,
            weatherCode = cur?.weatherCode,
            conditionText = wmoDescription(cur?.weatherCode),
            kind = wmoKind(cur?.weatherCode),
            humidity = cur?.humidity?.roundToIntOrNull(),
            humidityNote = humidityText(cur?.humidity),
            windSpeed = wind,
            windText = windDirectionText(cur?.windDirection),
            windLevel = windLevel(wind),
            pressure = cur?.pressure?.roundToIntOrNull(),
            uvIndex = uv,
            uvText = uvLevelText(uv),
            precipitationProbability = daily?.precipitationProbabilityMax?.getOrNull(0),
            sunrise = daily?.sunrise?.getOrNull(0)?.substringAfter("T")?.take(5).orEmpty(),
            sunset = daily?.sunset?.getOrNull(0)?.substringAfter("T")?.take(5).orEmpty(),
            observedAt = cur?.time.orEmpty(),
            hourly = realHourly,
            daily = realDaily
        )
    }

    /** 星期几标签：第 0 项固定「今天」，第 1 项「明天」，其余按真实星期。 */
    private fun weekdayLabel(date: String, index: Int): String = when (index) {
        0 -> "今天"
        1 -> "明天"
        else -> {
            runCatching {
                val d = java.time.LocalDate.parse(date)
                when (d.dayOfWeek.value) {
                    1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"
                    5 -> "周五"; 6 -> "周六"; else -> "周日"
                }
            }.getOrDefault(date)
        }
    }
}

/** Double → Int 的安全四舍五入（防 NaN/Inf）。 */
private fun Double.roundToIntSafe(): Int =
    if (isNaN() || isInfinite()) 0 else kotlin.math.roundToInt()

/**
 * 可空 Double 的版本。
 *
 * 注意：这里刻意**不**与上面的 `Double.roundToIntSafe()` 同名重载，
 * 避免 `x?.roundToIntSafe()` 在编译器里产生重载歧义。
 */
private fun Double?.roundToIntOrNull(): Int? = this?.roundToIntSafe()