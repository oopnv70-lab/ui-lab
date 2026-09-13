package com.oopnv70.uilab.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// =====================================================================
// 天气仓库：真实网络请求（Open-Meteo，免 Key）
// =====================================================================
// 为什么不用 Retrofit：
//   项目虽然引了 retrofit，但这两个都是极简 GET + JSON 响应，
//   用 HttpURLConnection + kotlinx.serialization 更少依赖、更好排查。
//   （retrofit 依赖仍在 libs 里，将来接更多源时再启用。）
//
// 所有方法都是 suspend，必须在 IO 线程调用（内部已 withContext 切换）。
// 失败一律返回 Result.failure，不抛异常给 UI。
// =====================================================================

/** 定好坐标与名字的地点（搜索或定位得来）。 */
data class GeoPlace(
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double
)

object WeatherRepository {

    private const val TAG = "UiLab.Weather"

    private const val GEO_URL = "https://geocoding-api.open-meteo.com/v1/search"
    private const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"

    /**
     * Nominatim（OpenStreetMap）地理编码。
     *
     * 为什么换它：Open-Meteo 的地名库中文覆盖不全 ——
     * 「淮南 / 许昌 / 宿州 / 安徽」直接 0 结果，「上海市」还会命中美国伊利诺伊州。
     * Nominatim 的中文来自 OSM 的 name:zh 标签，实测覆盖完整。
     *
     * ⚠️ 官方使用条款：
     *   1. 必须带可识别的 User-Agent（[httpGet] 里已统一带上）
     *   2. 限速 1 请求/秒 —— 所以调用方（搜索框）必须做防抖
     */
    private const val NOMINATIM_URL = "https://nominatim.openstreetmap.org/search"

    /** 宽松 JSON：忽略未知字段，缺失字段用默认值。 */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // -----------------------------------------------------------------
    // 1) 按关键词搜城市（不预设任何城市表）
    // -----------------------------------------------------------------

    /**
     * 用关键词搜索城市。
     *
     * 策略：**先问 Nominatim（中文覆盖好），没有再退回 Open-Meteo**。
     * 这样既拿到中文全面覆盖，也保留一个兜底源以防某个源临时抽风。
     *
     * @param keyword 用户输入，如「淮南」「深圳」「Tokyo」。
     * @param count 最多返回几条。
     * @return 匹配到的地点列表；两个源都失败或都没结果时返回空列表。
     */
    suspend fun searchCities(keyword: String, count: Int = 10): List<GeoPlace> {
        val q = keyword.trim()
        if (q.isEmpty()) return emptyList()

        // 首选 Nominatim
        val primary = searchCitiesNominatim(q, count)
        if (primary.isNotEmpty()) {
            Log.d(TAG, "搜索「$q」→ Nominatim ${primary.size} 条")
            return primary
        }

        // 兜底 Open-Meteo
        val fallback = searchCitiesOpenMeteo(q, count)
        Log.d(TAG, "搜索「$q」→ Nominatim 0 条，Open-Meteo 兜底 ${fallback.size} 条")
        return fallback
    }

    /** 用 Nominatim（OSM）搜城市。 */
    private suspend fun searchCitiesNominatim(q: String, count: Int): List<GeoPlace> = try {
        val url = "$NOMINATIM_URL?q=${enc(q)}&format=jsonv2&addressdetails=1" +
                "&accept-language=zh-CN&limit=$count"
        val body = httpGet(url)
        val raw = json.decodeFromString(
            ListSerializer(NominatimPlaceDto.serializer()), body
        )
        raw.asSequence()
            // 行政区结果优先：实测搜「许昌」第一条是**火车站**（addresstype=railway），
            // 直接取第一条会拿到车站而不是城市。这里把「城市 / 地区 / 省 / 国家」
            // 这类行政区排到前面，铁路 / 道路 / 建筑等排后面。
            .sortedByDescending { dto -> administrativeRank(dto.addressType) }
            .mapNotNull { dto ->
                val lat = dto.latitude
                val lon = dto.longitude
                val name = dto.name?.takeIf { it.isNotBlank() }
                    ?: dto.displayName?.substringBefore(',')?.trim()
                // lat/lon 解析失败、或没有名字的直接丢掉（不编造）
                if (lat == null || lon == null || name.isNullOrBlank()) return@mapNotNull null
                GeoPlace(
                    name = name,
                    subtitle = dto.buildSubtitle(),
                    latitude = lat,
                    longitude = lon
                )
            }
            .toList()
    } catch (t: Throwable) {
        Log.w(TAG, "Nominatim 搜索「$q」失败: ${t.message}")
        emptyList()
    }

    /** 用 Open-Meteo Geocoding 搜城市（兜底）。 */
    private suspend fun searchCitiesOpenMeteo(q: String, count: Int): List<GeoPlace> = try {
        val url = "$GEO_URL?name=${enc(q)}&count=$count&language=zh&format=json"
        val body = httpGet(url)
        val resp = json.decodeFromString(GeoSearchResponse.serializer(), body)
        resp.results.orEmpty()
            .filter { it.name.isNotBlank() }
            .map {
                GeoPlace(
                    name = it.name,
                    subtitle = it.subtitle,
                    latitude = it.latitude,
                    longitude = it.longitude
                )
            }
    } catch (t: Throwable) {
        Log.w(TAG, "Open-Meteo 搜索「$q」失败: ${t.message}")
        emptyList()
    }

    // -----------------------------------------------------------------
    // 2) 按坐标取真实天气
    // -----------------------------------------------------------------

    /**
     * 取某坐标的真实天气。
     *
     * @return 成功时是 [RealWeather]；失败时 Result.failure。
     */
    suspend fun fetchWeather(
        latitude: Double,
        longitude: Double
    ): Result<RealWeather> = withContext(Dispatchers.IO) {
        val url = buildString {
            append(FORECAST_URL)
            append("?latitude=").append(latitude)
            append("&longitude=").append(longitude)
            append("&current=temperature_2m,relative_humidity_2m,apparent_temperature,")
            append("is_day,precipitation,weather_code,wind_speed_10m,")
            append("wind_direction_10m,surface_pressure")
            append("&hourly=temperature_2m,precipitation_probability,weather_code")
            append("&daily=weather_code,temperature_2m_max,temperature_2m_min,")
            append("precipitation_probability_max,sunrise,sunset,uv_index_max")
            append("&timezone=auto")
            append("&forecast_days=7")
        }
        try {
            val body = httpGet(url)
            val dto = json.decodeFromString(ForecastResponse.serializer(), body)
            val real = RealWeatherFactory.from(dto)
            Log.d(
                TAG,
                "天气 OK: ${real.temperature}° ${real.conditionText} " +
                        "(体感 ${real.feelsLike}°, ${real.high}/${real.low}°)"
            )
            Result.success(real)
        } catch (t: Throwable) {
            Log.w(TAG, "取天气失败: ${t.message}")
            Result.failure(t)
        }
    }

    // -----------------------------------------------------------------
    // 3) 一步到位：搜到第一个匹配 → 直接取它的天气
    // -----------------------------------------------------------------

    /**
     * 按关键词搜城市并顺带取回该地真实天气。
     *
     * 供「添加城市」用：用户输入「深圳」，直接拿到深圳的真实温度。
     */
    suspend fun searchCityWithWeather(keyword: String): Result<Pair<GeoPlace, RealWeather>> {
        val hits = searchCities(keyword, count = 1)
        val place = hits.firstOrNull()
            ?: return Result.failure(IllegalStateException("没有找到「$keyword」"))
        return fetchWeather(place.latitude, place.longitude).map { place to it }
    }

    // -----------------------------------------------------------------
    // 内部：最朴素的 GET
    // -----------------------------------------------------------------

    /** URL 编码（中文关键词必须编码）。 */
    private fun enc(s: String): String =
        URLEncoder.encode(s, "UTF-8")

    /** 发一个 GET，返回响应体字符串；非 2xx 抛异常。 */
    private suspend fun httpGet(urlStr: String): String = withContext(Dispatchers.IO) {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "ui-lab/0.5 (Android; weather app)")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}