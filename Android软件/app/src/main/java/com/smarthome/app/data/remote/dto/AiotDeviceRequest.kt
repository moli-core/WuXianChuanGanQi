package com.smarthome.app.data.remote.dto

import com.squareup.moshi.Json

data class AiotDeviceRequest(
    @Json(name = "deviceCode") val deviceCode: String,
    @Json(name = "deviceName") val deviceName: String,
    @Json(name = "deviceType") val deviceType: String? = null,
    @Json(name = "ipAddress") val ipAddress: String? = null,
    @Json(name = "macAddress") val macAddress: String? = null,
    @Json(name = "mqttTopic") val mqttTopic: String? = null,
    @Json(name = "sensorList") val sensorList: String? = null,
    @Json(name = "remarks") val remarks: String? = null
)
