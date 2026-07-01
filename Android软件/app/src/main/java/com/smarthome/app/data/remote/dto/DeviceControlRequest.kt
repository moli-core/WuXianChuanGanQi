package com.smarthome.app.data.remote.dto

import com.squareup.moshi.Json

data class DeviceControlRequest(
    @Json(name = "deviceCode") val deviceCode: String,
    @Json(name = "action") val action: Int
)
