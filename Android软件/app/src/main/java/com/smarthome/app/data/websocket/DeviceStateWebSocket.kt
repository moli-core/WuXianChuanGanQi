package com.smarthome.app.data.websocket

import com.smarthome.app.core.model.WebSocketMessage
import com.smarthome.app.core.network.WebSocketManager
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*

class DeviceStateWebSocket(
    private val webSocketManager: WebSocketManager,
    private val moshi: Moshi
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val jsonAdapter = moshi.adapter(WebSocketMessage::class.java)

    val deviceStateFlow: SharedFlow<WebSocketMessage> = webSocketManager.messages
        .mapNotNull { rawJson ->
            try { jsonAdapter.fromJson(rawJson) } catch (e: Exception) { null }
        }
        .shareIn(scope, SharingStarted.WhileSubscribed(5000), replay = 1)
}
