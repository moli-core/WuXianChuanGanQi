package com.smarthome.app.core.model

import com.squareup.moshi.Json

data class VoiceResult(
    @Json(name = "voiceText") val voiceText: String = "",
    @Json(name = "controlCmd") val controlCmd: String? = null,
    @Json(name = "cloudSent") val cloudSent: Boolean? = null
)
