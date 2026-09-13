package com.oopnv70.uilab.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =====================================================================
// Nominatim（OpenStreetMap）地理编码 DTO
// =====================================================================
// 为什么从 Open-Meteo Geocoding 换成 Nominatim：
//
//   Open-Meteo 的地名库（源自 GeoNames）对**中文覆盖不完整**。实测：
//     搜「淮南」「淮南市」「许昌」「宿州」「安徽」→ 0 结果
//     搜「上海市」→ 命中的是**美国伊利诺伊州**的 Shanghai（错！）
//     搜「阜阳」→ 返回的是**江苏**（实际应是安徽，数据错）
//
//   Nominatim 的中文名来自 OSM 的 name:zh 标签，实测 20/20 全中，
//   且行政区划层级完整（省 / 市 / 区）。
//
// 接口（GET，免 Key）：
//   https://nominatim.openstreetmap.org/search
//     ?q=淮南&format=jsonv2&accept-language=zh-CN&limit=10
//
// ⚠️ 两条硬约束（不遵守会被封 IP）：
//   1. **必须**带可识别的 User-Agent（本项目的 httpGet 已带 "ui-lab/..."）
//   2. 限速 **1 请求/秒** —— 所以搜索输入必须防抖，不能每次按键都发
//
// 响应是一个**顶层 JSON 数组**（不是对象包 results），且 lat/lon 是**字符串**。
// =====================================================================

/** Nominatim 返回的一个地点。字段很多，只取要用的。 */
@Serializable
data class NominatimPlaceDto(
    /** 完整地址串，如「淮南市, 安徽省, 202033, 中国」。 */
    @SerialName("display_name") val displayName: String? = null,
    /** 纬度（**字符串**！Nominatim 用的是字符串）。 */
    @SerialName("lat") val lat: String? = null,
    /** 经度（字符串）。 */
    @SerialName("lon") val lon: String? = null,
    /** 地名，如「淮南市」。jsonv2 才有。 */
    @SerialName("name") val name: String? = null,
    /** 类型，如 city / administrative / station。 */
    @SerialName("type") val type: String? = null,
    /**
     * 类别，如 boundary / place / railway。
     *
     * ⚠️ 字段名是 **category** 不是 `class`（`class` 是旧版 JSONv1 的名字，
     * jsonv2 改成了 category）。实测确认过，别写成 class。
     */
    @SerialName("category") val category: String? = null,
    /**
     * 「这个结果算什么」，如 city / region / state / railway / road。
     * 用来过滤掉非行政区结果（实测搜「许昌」第一条是**火车站**）。
     */
    @SerialName("addresstype") val addressType: String? = null,
    /** 重要度，Nominatim 自己的排序权重（越大越重要）。 */
    @SerialName("importance") val importance: Double? = null,
    /** 结构化地址（需请求时带 addressdetails=1，否则为 null）。 */
    @SerialName("address") val address: NominatimAddressDto? = null
) {
    /** 解析后的纬度；非法返回 null（不编造数字）。 */
    val latitude: Double?
        get() = lat?.toDoubleOrNull()

    /** 解析后的经度；非法返回 null。 */
    val longitude: Double?
        get() = lon?.toDoubleOrNull()

    /**
     * 组装给 UI 看的副标题，尽量给出「省 · 国家」。
     *
     * 优先用结构化 address（请求时带了 addressdetails=1）；
     * 拿不到就退回 display_name 里去掉第一段的部分。
     * 不编造：全都没有就返回空串。
     */
    fun buildSubtitle(): String {
        val a = address
        val parts = listOfNotNull(
            a?.state?.takeIf { it.isNotBlank() },
            a?.region?.takeIf { it.isNotBlank() },
            a?.country?.takeIf { it.isNotBlank() }
        )
        if (parts.isNotEmpty()) return parts.distinct().joinToString(" · ")

        // 退回 display_name：「淮南市, 安徽省, 202033, 中国」→「安徽省 · 中国」
        val dn = displayName ?: return ""
        val rest = dn.split(',').map { it.trim() }.filter { it.isNotBlank() }
        if (rest.size <= 1) return ""
        // 去掉首段（地名），末尾若是纯数字（邮编）也去掉
        return rest.drop(1)
            .filterNot { it.all { c -> c.isDigit() } }
            .distinct()
            .joinToString(" · ")
    }
}

/**
 * 把 Nominatim 的 `addresstype` 折算成优先级，越大越「像行政区」。
 *
 * 起因：实测搜「许昌」，Nominatim 第一条返回的是**许昌站**（addresstype=railway），
 * 而城市本身排在后面。直接取第一条会把火车站当成城市。
 * 这里让行政区排在前面，确保默认选中项是城市而不是某个建筑/车站。
 */
internal fun administrativeRank(addressType: String?): Int = when (addressType?.lowercase()) {
    "country" -> 100
    "state", "province", "region" -> 90
    "city" -> 80
    "town", "municipality" -> 70
    "county", "district", "suburb", "borough", "city_district" -> 60
    "village", "hamlet" -> 50
    "road", "street" -> 20
    "railway", "station", "aerodrome", "airport" -> 10
    null, "" -> 30   // 没标注时给个中间值，不刻意打压
    else -> 30
}

/** 结构化地址。字段名随国家不同，能取到几个算几个。 */
@Serializable
data class NominatimAddressDto(
    @SerialName("city") val city: String? = null,
    @SerialName("town") val town: String? = null,
    @SerialName("county") val county: String? = null,
    /** 一级行政区，中文如「安徽省」。 */
    @SerialName("state") val state: String? = null,
    /** 某些地区用 region（实测「许昌市」就在 region 里）。 */
    @SerialName("region") val region: String? = null,
    /** 国家，中文如「中国」。 */
    @SerialName("country") val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null
)