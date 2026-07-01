package com.smarthome.app.core.model

import com.squareup.moshi.Json

data class DeviceStatus(
    @Json(name = "deviceCode") val deviceCode: String = "",
    @Json(name = "deviceName") val deviceName: String = "",
    @Json(name = "status") val status: Int = 0,
    @Json(name = "type") val type: String? = null,
    @Json(name = "updateTime") val updateTime: String? = null
)
