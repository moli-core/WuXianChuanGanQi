package com.smarthome.app.core.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 设备状态变更事件总线
 * AI 对话控制设备后通知其他页面刷新
 */
object DeviceEventBus {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val events: SharedFlow<String> = _events.asSharedFlow()

    suspend fun post(deviceCode: String) {
        _events.emit(deviceCode)
    }
}
