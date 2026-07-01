package com.smarthome.app.core.model

import com.squareup.moshi.Json

data class AiotDevice(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "deviceCode") val deviceCode: String = "",
    @Json(name = "deviceName") val deviceName: String = "",
    @Json(name = "deviceType") val deviceType: String? = null,
    @Json(name = "ipAddress") val ipAddress: String? = null,
    @Json(name = "macAddress") val macAddress: String? = null,
    @Json(name = "mqttTopic") val mqttTopic: String? = null,
    @Json(name = "firmwareVersion") val firmwareVersion: String? = null,
    @Json(name = "sensorList") val sensorList: String? = null,
    @Json(name = "ledStatus") val ledStatus: Int? = null,
    @Json(name = "mode") val mode: String? = null,
    @Json(name = "lightSensor") val lightSensor: Int? = null,
    @Json(name = "pirStatus") val pirStatus: Int? = null,
    @Json(name = "screenStatus") val screenStatus: Int? = null,
    @Json(name = "wifiRssi") val wifiRssi: Int? = null,
    @Json(name = "tcpConnected") val tcpConnected: Boolean? = null,
    @Json(name = "onlineStatus") val onlineStatus: Boolean? = null,
    @Json(name = "lastReportTime") val lastReportTime: String? = null,
    @Json(name = "registerSource") val registerSource: String? = null,
    @Json(name = "remarks") val remarks: String? = null,
    @Json(name = "createTime") val createTime: String? = null,
    @Json(name = "updateTime") val updateTime: String? = null
)
