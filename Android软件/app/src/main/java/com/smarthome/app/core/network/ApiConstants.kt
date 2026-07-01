package com.smarthome.app.core.network

object ApiConstants {
    // Auth
    const val AUTH_LOGIN = "/api/auth/login"
    const val AUTH_REGISTER = "/api/auth/register"
    const val AUTH_PROFILE = "/api/auth/profile"

    // Device Control
    const val DEVICE_STATUS = "/api/device/status"
    const val DEVICE_STATUS_SINGLE = "/api/device/status/{deviceCode}"
    const val DEVICE_CONTROL = "/api/device/control"
    const val DEVICE_ALL_ON = "/api/device/all-on"
    const val DEVICE_ALL_OFF = "/api/device/all-off"

    // Data / Dashboard
    const val DATA_DASHBOARD = "/api/data/dashboard"
    const val DATA_LATEST = "/api/data/latest"
    const val DATA_CHART = "/api/data/chart"

    // AI Chat
    const val AI_CHAT = "/api/ai/chat"

    // Weather
    const val WEATHER_CURRENT = "/api/weather/current"

    // Voice
    const val VOICE_UPLOAD = "/api/voice/upload"

    // AIoT Device Management
    const val AIOT_DEVICE = "/api/aiot-device"
    const val AIOT_DEVICE_SINGLE = "/api/aiot-device/{deviceCode}"

    // Reports
    const val REPORT_DEVICE_STATS = "/api/report/device-stats"
    const val REPORT_EVENT_TRENDS = "/api/report/event-trends"
    const val REPORT_EVENT_DISTRIBUTION = "/api/report/event-distribution"
    const val REPORT_DEVICE_RANKING = "/api/report/device-ranking"

    // Logs
    const val LOG_VOICE = "/api/log/voice"
    const val LOG_OPERATION = "/api/log/operation"
    const val LOG_ALERT = "/api/log/alert"

    // WebSocket
    const val WS_DEVICE_STATE = "/ws/device-state"
}
