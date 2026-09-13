package com.oopnv70.uilab.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 依赖层自检用的最小模型。
 *
 * 目的：验证当前构建环境（AGP 9 内置 Kotlin + kotlinx.serialization 插件）
 * 能够正常生成序列化器并完成解析。业务模型落地后会删除本文件。
 */
@Serializable
private data class SelfCheckDto(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("weather_code") val weatherCode: Int
)

/** 供启动时调用，确认序列化链路可用（仅 DEBUG 下调用）。 */
internal object DependencySelfCheck {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 用一段真实 Open-Meteo 响应片段做解析自检。
     * @return 解析出的温度，失败时返回 null。
     */
    fun parseSample(): Double? = runCatching {
        val sample = """{"temperature_2m":24.5,"weather_code":0}"""
        json.decodeFromString<SelfCheckDto>(sample).temperature
    }.getOrNull()
}