package com.smarthome.app.core.model

import com.squareup.moshi.Json

/**
 * WebSocket 推送的设备状态消息
 * 格式: {"Status_LED":true, "Status_beeper":true, "Data_temp":26.5, "Data_humi":62, "Status_body":0}
 */
data class WebSocketMessage(
    @Json(name = "Status_LED") val statusLed: Boolean? = null,
    @Json(name = "Status_beeper") val statusBeeper: Boolean? = null,
    @Json(name = "Data_temp") val dataTemp: Double? = null,
    @Json(name = "Data_humi") val dataHumi: Double? = null,
    @Json(name = "Status_body") val statusBody: Int? = null
) {
    fun toDeviceStatusMap(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        statusLed?.let { map["led"] = if (it) 1 else 0 }
        statusBeeper?.let { map["buzzer"] = if (it) 1 else 0 }
        // ledRed and ledYellow are not in WS by default, keep as is
        return map
    }
}
