package com.oopnv70.uilab.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

// =====================================================================
// Photon（Komoot / OpenStreetMap）反向地理编码
// =====================================================================
// 为什么引入 Photon：
//
//   定位链路原本用系统 Geocoder 做「坐标 → 地名」，但国内无 GMS 的机型
//   Geocoder 常常只给到「市」一级，拿不到区 / 街道，精度上不去。
//
//   Photon 是基于 OSM 的免 Key 地理编码服务，反向接口实测（2026-09）：
//     30.2741,120.1551 → 杭州市 / 天水街道 / 体育场路 / 凌云航空大酒店 / 310006
//   即：**免费直连、返回门牌级**，正好补上系统 Geocoder 缺的那几级。
//
//   另一个候选 Nominatim 在同一网络环境里 DNS 只解析到 IPv6 且 443 不通，
//   但那是网络环境差异，不是服务本身的结论 —— 保留 Photon 作为独立增强层，
//   失败时静默退回系统 Geocoder，不因网络抖动破坏现有定位。
//
// 接口（GET，免 Key）：
//   https://photon.komoot.io/reverse?lat=30.2741&lon=120.1551&lang=zh
//
// 响应是 GeoJSON FeatureCollection：features[0].properties 里带
// name / street / district / city / state / country / postcode 等。
// =====================================================================

/** Photon 反向地理编码响应（GeoJSON FeatureCollection）。 */
@Serializable
data class PhotonReverseResponse(
    @SerialName("features") val features: List<PhotonFeatureDto> = emptyList()
)

@Serializable
data class PhotonFeatureDto(
    @SerialName("properties") val properties: PhotonPropertiesDto? = null
)

@Serializable
data class PhotonPropertiesDto(
    /** 具体地点名，如「凌云航空大酒店」。 */
    @SerialName("name") val name: String? = null,
    /** 街道 / 道路，如「体育场路」。 */
    @SerialName("street") val street: String? = null,
    /** 街道号。 */
    @SerialName("housenumber") val housenumber: String? = null,
    /** 区 / 街道级，如「天水街道」。 */
    @SerialName("district") val district: String? = null,
    /** 市，如「杭州市」。 */
    @SerialName("city") val city: String? = null,
    /** 镇 / 县（部分结果放在这里而不是 district/city）。 */
    @SerialName("county") val county: String? = null,
    /** 省 / 州，如「浙江省」。 */
    @SerialName("state") val state: String? = null,
    /** 国家，如「中国」。 */
    @SerialName("country") val country: String? = null,
    /** 邮编，如「310006」。 */
    @SerialName("postcode") val postcode: String? = null
) {
    /**
     * 折算成 [ReversePlace]，字段已经对齐 [com.oopnv70.uilab.location.LocatedPlace]
     * 的「省 / 市 / 区 / 街道 / 门牌」五级。
     *
     * 注意：Photon 的 `district` 在中国数据里常是**街道**级（如「天水街道」），
     * 而系统的 subLocality 也是「区/县」。这里做一次规整，避免把「街道」塞进
     * 「区」字段导致 UI 显示错乱：
     *   - district 结尾是「街道」→ 归到 street 语义那一档
     *   - 否则 → 归到 district（区/县）那一档
     */
    fun toReversePlace(): ReversePlace = ReversePlace(
        province = state,
        city = city ?: county,
        district = district?.takeUnless { it.endsWith("街道") },
        street = street ?: district?.takeIf { it.endsWith("街道") },
        feature = listOfNotNull(name, housenumber)
            .distinct()
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
    )
}

/** 归一化后的反向地理编码结果，字段语义与 LocatedPlace 一致。 */
data class ReversePlace(
    val province: String? = null,
    val city: String? = null,
    val district: String? = null,
    val street: String? = null,
    val feature: String? = null
)

/** Photon 反向地理编码客户端。失败统一返回 null，不抛异常给上层。 */
object PhotonReverseGeocoder {

    private const val TAG = "UiLab.Photon"
    private const val PHOTON_REVERSE_URL = "https://photon.komoot.io/reverse"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * 坐标 → 地名。
     *
     * @return 成功返回 [ReversePlace]（字段可为空）；网络/解析失败返回 null。
     */
    suspend fun reverse(latitude: Double, longitude: Double): ReversePlace? = try {
        val url = "$PHOTON_REVERSE_URL?lat=$latitude&lon=$longitude&lang=zh"
        val body = httpGet(url)
        val resp = json.decodeFromString(PhotonReverseResponse.serializer(), body)
        val props = resp.features.firstOrNull()?.properties
        val place = props?.toReversePlace()
        Log.d(
            TAG,
            "反解 $latitude,$longitude → 省=${place?.province} 市=${place?.city} " +
                "区=${place?.district} 街道=${place?.street} 门牌=${place?.feature}"
        )
        place
    } catch (t: Throwable) {
        Log.w(TAG, "Photon 反解失败: ${t.message}")
        null
    }

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