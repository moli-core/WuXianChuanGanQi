package com.smarthome.app.core.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*

class WebSocketManager(private val okHttpClient: OkHttpClient) {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var shouldReconnect = false
    private var currentHost: String = ""

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun connect(host: String, port: Int = 8080) {
        currentHost = host
        shouldReconnect = true
        val wsUrl = "ws://$host:$port${ApiConstants.WS_DEVICE_STATE}"
        val request = Request.Builder().url(wsUrl).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) { _messages.tryEmit(text) }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { if (shouldReconnect) scheduleReconnect() }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { if (shouldReconnect) scheduleReconnect() }
        })
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        webSocket?.close(1000, "App closing")
        webSocket = null
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(5000)
            if (shouldReconnect && currentHost.isNotEmpty()) connect(currentHost)
        }
    }
}
