package com.smarthome.app.core.model

import com.squareup.moshi.Json

data class DashboardVO(
    @Json(name = "currentTemp") val currentTemp: Double? = null,
    @Json(name = "currentHumidity") val currentHumidity: Double? = null,
    @Json(name = "currentSmoke") val currentSmoke: Double? = null,
    @Json(name = "deviceStatus") val deviceStatus: Map<String, Int>? = null,
    @Json(name = "todayAlertCount") val todayAlertCount: Int = 0,
    @Json(name = "todayVoiceCount") val todayVoiceCount: Int = 0
)
