package com.smarthome.app.core.model

import com.squareup.moshi.Json

data class EnvData(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "deviceCode") val deviceCode: String? = null,
    @Json(name = "temperature") val temperature: Double? = null,
    @Json(name = "humidity") val humidity: Double? = null,
    @Json(name = "smokeLevel") val smokeLevel: Double? = null,
    @Json(name = "createTime") val createTime: String? = null
)

data class EnvChartVO(
    @Json(name = "times") val times: List<String> = emptyList(),
    @Json(name = "temperatures") val temperatures: List<Double> = emptyList(),
    @Json(name = "humidities") val humidities: List<Double> = emptyList(),
    @Json(name = "smokeLevels") val smokeLevels: List<Double> = emptyList(),
    @Json(name = "summary") val summary: String? = null
)
