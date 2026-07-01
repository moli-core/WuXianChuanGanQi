package com.smarthome.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.core.model.ChatMessageUi
import com.smarthome.app.core.model.MessageType
import com.smarthome.app.core.model.WeatherData
import com.smarthome.app.core.util.CommandParser
import com.smarthome.app.core.util.DeviceEventBus
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.repository.AiRepository
import com.smarthome.app.data.repository.DeviceRepository
import com.smarthome.app.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiChatUiState(
    val messages: List<ChatMessageUi> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class AiChatViewModel(
    private val aiRepository: AiRepository,
    private val deviceRepository: DeviceRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val question = _uiState.value.inputText.trim()
        if (question.isEmpty()) return

        // 先添加用户消息
        _uiState.update { state ->
            state.copy(
                messages = state.messages + ChatMessageUi(question, isUser = true),
                inputText = "",
                isLoading = true,
                error = null
            )
        }

        // 1. 尝试解析设备控制命令
        val deviceCmd = CommandParser.parseDeviceCommand(question)
        if (deviceCmd != null) {
            executeDeviceCommand(deviceCmd.displayName, deviceCmd.deviceCode, deviceCmd.action)
            return
        }

        // 2. 尝试解析天气查询
        val city = CommandParser.parseWeatherQuery(question)
        if (city != null) {
            queryWeather(city)
            return
        }

        // 3. 默认走 AI 聊天
        aiChat(question)
    }

    private fun executeDeviceCommand(displayName: String, deviceCode: String, action: Int) {
        val actionText = if (action == 1) "打开" else "关闭"
        viewModelScope.launch {
            val result = deviceRepository.control(deviceCode, action)
            // 通知其他页面刷新设备状态
            DeviceEventBus.post(deviceCode)
            val reply = when (result) {
                is NetworkResult.Success -> "✅ 已${actionText}${displayName}"
                is NetworkResult.Error -> "❌ ${actionText}${displayName}失败：${result.message}"
                is NetworkResult.Exception -> "❌ ${actionText}${displayName}失败：${result.e.localizedMessage}"
                else -> "❌ 操作失败"
            }
            _uiState.update { state ->
                state.copy(
                    messages = state.messages + ChatMessageUi(
                        content = reply,
                        isUser = false,
                        type = MessageType.DEVICE_ACTION
                    ),
                    isLoading = false
                )
            }
        }
    }

    private fun queryWeather(city: String) {
        viewModelScope.launch {
            when (val result = weatherRepository.getCurrentWeather(city)) {
                is NetworkResult.Success -> {
                    val w = result.data
                    val reply = formatWeatherReply(w, city)
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + ChatMessageUi(
                                content = reply,
                                isUser = false,
                                type = MessageType.WEATHER
                            ),
                            isLoading = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + ChatMessageUi(
                                content = "❌ 查询${city}天气失败：${result.message}",
                                isUser = false
                            ),
                            isLoading = false
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + ChatMessageUi(
                                content = "❌ 查询${city}天气失败：${result.e.localizedMessage}",
                                isUser = false
                            ),
                            isLoading = false
                        )
                    }
                }
                else -> {}
            }
        }
    }

    private fun aiChat(question: String) {
        viewModelScope.launch {
            when (val result = aiRepository.chat(question)) {
                is NetworkResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + ChatMessageUi(
                                content = result.data.reply.ifEmpty { "暂无回复" },
                                isUser = false
                            ),
                            isLoading = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false, error = "网络连接失败") }
                }
                else -> {}
            }
        }
    }

    private fun formatWeatherReply(w: WeatherData, city: String): String {
        val sb = StringBuilder()
        sb.appendLine("🌤 **${w.city ?: city}** 天气")
        sb.appendLine("温度：${w.temperature ?: "--"}°C")
        sb.appendLine("天气：${w.weather ?: "--"}")
        sb.appendLine("湿度：${w.humidity ?: "--"}")
        sb.append("更新时间：${w.updateTime ?: "--"}")
        return sb.toString()
    }
}
