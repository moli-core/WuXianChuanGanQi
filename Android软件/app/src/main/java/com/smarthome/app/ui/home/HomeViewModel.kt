package com.smarthome.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.core.model.WebSocketMessage
import com.smarthome.app.core.util.Constants
import com.smarthome.app.core.util.DeviceEventBus
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.repository.DashboardRepository
import com.smarthome.app.data.repository.DeviceRepository
import com.smarthome.app.data.websocket.DeviceStateWebSocket
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val temperature: String = "--",
    val humidity: String = "--",
    val smokeLevel: String = "--",
    val deviceLed: Int = 0,
    val deviceLedRed: Int = 0,
    val deviceLedYellow: Int = 0,
    val deviceBuzzer: Int = 0,
    val devicePir: Int = 0,
    val todayAlertCount: Int = 0,
    val todayVoiceCount: Int = 0,
    val isRefreshing: Boolean = false
)

class HomeViewModel(
    private val dashboardRepository: DashboardRepository,
    private val deviceRepository: DeviceRepository,
    private val deviceStateWebSocket: DeviceStateWebSocket
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        observeWebSocket()
        // 监听设备控制事件，自动刷新
        viewModelScope.launch {
            DeviceEventBus.events.collect { loadDashboard() }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadDashboard()
    }

    fun toggleDevice(deviceCode: String, currentStatus: Int) {
        val action = if (currentStatus == Constants.ACTION_ON) Constants.ACTION_OFF else Constants.ACTION_ON
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            when (val result = deviceRepository.control(deviceCode, action)) {
                is NetworkResult.Success -> { DeviceEventBus.post(deviceCode) }
                else -> loadDashboard()
            }
        }
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = dashboardRepository.getDashboard()) {
                is NetworkResult.Success -> {
                    val data = result.data
                    val deviceMap = data.deviceStatus ?: emptyMap()
                    _uiState.update {
                        it.copy(
                            isLoading = false, isRefreshing = false,
                            temperature = data.currentTemp?.let { String.format("%.1f", it) } ?: "--",
                            humidity = data.currentHumidity?.let { String.format("%.1f", it) } ?: "--",
                            smokeLevel = data.currentSmoke?.let { String.format("%.0f", it) } ?: "--",
                            deviceLed = deviceMap[Constants.DEVICE_LED] ?: 0,
                            deviceLedRed = deviceMap[Constants.DEVICE_LED_RED] ?: 0,
                            deviceLedYellow = deviceMap[Constants.DEVICE_LED_YELLOW] ?: 0,
                            deviceBuzzer = deviceMap[Constants.DEVICE_BUZZER] ?: 0,
                            devicePir = deviceMap[Constants.DEVICE_PIR] ?: 0,
                            todayAlertCount = data.todayAlertCount,
                            todayVoiceCount = data.todayVoiceCount
                        )
                    }
                }
                is NetworkResult.Error -> { _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = result.message) } }
                is NetworkResult.Exception -> { _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = result.e.localizedMessage) } }
                else -> {}
            }
        }
    }

    private fun observeWebSocket() {
        viewModelScope.launch {
            deviceStateWebSocket.deviceStateFlow.collect { msg: WebSocketMessage ->
                _uiState.update { state ->
                    state.copy(
                        temperature = msg.dataTemp?.let { String.format("%.1f", it) } ?: state.temperature,
                        humidity = msg.dataHumi?.let { String.format("%.1f", it) } ?: state.humidity,
                        deviceLed = msg.statusLed?.let { if (it) 1 else 0 } ?: state.deviceLed,
                        deviceBuzzer = msg.statusBeeper?.let { if (it) 1 else 0 } ?: state.deviceBuzzer,
                        devicePir = msg.statusBody?.let { if (it > 0) 1 else 0 } ?: state.devicePir
                    )
                }
            }
        }
    }
}
