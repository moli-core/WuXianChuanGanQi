package com.smarthome.app.core.model

import com.squareup.moshi.Json

data class DeviceStats(
    @Json(name = "totalDevices") val totalDevices: Int = 0,
    @Json(name = "onlineDevices") val onlineDevices: Int = 0,
    @Json(name = "todayVoiceCount") val todayVoiceCount: Int = 0,
    @Json(name = "todayAlertCount") val todayAlertCount: Int = 0
)

data class EventTrends(
    @Json(name = "dates") val dates: List<String> = emptyList(),
    @Json(name = "values") val values: List<Int> = emptyList()
)

data class EventDistribution(
    @Json(name = "names") val names: List<String> = emptyList(),
    @Json(name = "values") val values: List<Int> = emptyList(),
    @Json(name = "percentages") val percentages: List<Double>? = null
)

data class DeviceRanking(
    @Json(name = "deviceName") val deviceName: String = "",
    @Json(name = "count") val count: Int = 0
)
