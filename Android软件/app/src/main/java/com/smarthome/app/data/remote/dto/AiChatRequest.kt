package com.smarthome.app.data.remote.dto

import com.squareup.moshi.Json

data class AiChatRequest(
    @Json(name = "question") val question: String,
    @Json(name = "sessionId") val sessionId: String? = null
)
