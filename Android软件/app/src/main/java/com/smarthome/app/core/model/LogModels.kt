package com.smarthome.app.core.model

import com.squareup.moshi.Json

data class VoiceLogItem(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "rawText") val rawText: String = "",
    @Json(name = "semanticResult") val semanticResult: String? = null,
    @Json(name = "deviceCommand") val deviceCommand: String? = null,
    @Json(name = "isValid") val isValid: Boolean? = null,
    @Json(name = "parseMethod") val parseMethod: String? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "operator") val operator: String? = null,
    @Json(name = "ttsResponse") val ttsResponse: String? = null,
    @Json(name = "createTime") val createTime: String? = null
)

data class OperationLogItem(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "deviceCode") val deviceCode: String? = null,
    @Json(name = "deviceName") val deviceName: String? = null,
    @Json(name = "action") val action: String? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "operator") val operator: String? = null,
    @Json(name = "remark") val remark: String? = null,
    @Json(name = "createTime") val createTime: String? = null
)

data class AlertLogItem(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "deviceCode") val deviceCode: String? = null,
    @Json(name = "alertType") val alertType: String? = null,
    @Json(name = "alertLevel") val alertLevel: Int = 1,
    @Json(name = "content") val content: String? = null,
    @Json(name = "envValue") val envValue: Double? = null,
    @Json(name = "isHandled") val isHandled: Boolean = false,
    @Json(name = "handledBy") val handledBy: String? = null,
    @Json(name = "handledTime") val handledTime: String? = null,
    @Json(name = "createTime") val createTime: String? = null
)
