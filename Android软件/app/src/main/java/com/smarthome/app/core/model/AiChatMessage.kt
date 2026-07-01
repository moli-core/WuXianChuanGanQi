package com.smarthome.app.core.model

import com.squareup.moshi.Json

data class AiChatMessage(
    @Json(name = "question") val question: String = "",
    @Json(name = "reply") val reply: String = "",
    @Json(name = "sessionId") val sessionId: String? = null
)

enum class MessageType { TEXT, DEVICE_ACTION, WEATHER }

data class ChatMessageUi(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT
)
