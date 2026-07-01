package com.smarthome.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    // Auth
    data object Login : Screen("login", "登录")
    data object Register : Screen("register", "注册")
    data object ForgotPassword : Screen("forgot_password", "找回密码")

    // Main tabs
    data object Home : Screen("home", "首页")
    data object DeviceControl : Screen("device", "设备控制")
    data object Chat : Screen("chat", "AI对话")
    data object Report : Screen("report", "报表")
    data object Profile : Screen("profile", "我的")

    // Sub pages
    data object Weather : Screen("weather", "天气")
    data object VoiceControl : Screen("voice", "语音控制")
    data object AiotDeviceList : Screen("aiot-devices", "设备管理")
    data object Logs : Screen("logs", "日志记录")
    data object ProfileEdit : Screen("profile_edit", "编辑资料")

    data object AiotDeviceDetail : Screen("aiot-device/{deviceCode}", "设备详情") {
        fun createRoute(deviceCode: String) = "aiot-device/$deviceCode"
    }
    data object AiotDeviceForm : Screen("aiot-device-form?id={id}", "添加设备") {
        fun createRoute(id: Long? = null) = if (id != null) "aiot-device-form?id=$id" else "aiot-device-form"
    }
}
