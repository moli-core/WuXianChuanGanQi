package com.smarthome.app.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.core.model.DeviceStatus
import com.smarthome.app.core.util.Constants
import com.smarthome.app.core.util.DeviceEventBus
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceListUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val devices: List<DeviceStatus> = emptyList(),
    val isOperating: Boolean = false
)

class DeviceListViewModel(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceListUiState())
    val uiState: StateFlow<DeviceListUiState> = _uiState.asStateFlow()

    init {
        loadDeviceStatus()
        viewModelScope.launch {
            DeviceEventBus.events.collect { loadDeviceStatus() }
        }
    }

    fun loadDeviceStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = deviceRepository.getAllStatus()) {
                is NetworkResult.Success -> {
                    val filteredDevices = result.data.filter { it.deviceCode != "fan" && it.deviceCode != "DEVICE_FAN" }
                    _uiState.update { it.copy(isLoading = false, isOperating = false, devices = filteredDevices) }
                }
                is NetworkResult.Error -> { _uiState.update { it.copy(isLoading = false, isOperating = false, error = result.message) } }
                is NetworkResult.Exception -> { _uiState.update { it.copy(isLoading = false, isOperating = false, error = result.e.localizedMessage) } }
                else -> {}
            }
        }
    }

    fun toggleDevice(deviceCode: String, currentStatus: Int) {
        val action = if (currentStatus == Constants.ACTION_ON) Constants.ACTION_OFF else Constants.ACTION_ON
        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true) }
            when (val result = deviceRepository.control(deviceCode, action)) {
                is NetworkResult.Success -> { DeviceEventBus.post(deviceCode); loadDeviceStatus() }
                is NetworkResult.Error -> { _uiState.update { it.copy(isOperating = false, error = result.message) }; loadDeviceStatus() }
                is NetworkResult.Exception -> { _uiState.update { it.copy(isOperating = false, error = result.e.localizedMessage) } }
                else -> _uiState.update { it.copy(isOperating = false) }
            }
        }
    }

    fun allOn() {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true) }
            when (val result = deviceRepository.allOn()) {
                is NetworkResult.Success -> { DeviceEventBus.post("all"); loadDeviceStatus() }
                is NetworkResult.Error -> { _uiState.update { it.copy(isOperating = false, error = result.message) }; loadDeviceStatus() }
                is NetworkResult.Exception -> { _uiState.update { it.copy(isOperating = false, error = result.e.localizedMessage) } }
                else -> _uiState.update { it.copy(isOperating = false) }
            }
        }
    }

    fun allOff() {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true) }
            when (val result = deviceRepository.allOff()) {
                is NetworkResult.Success -> { DeviceEventBus.post("all"); loadDeviceStatus() }
                is NetworkResult.Error -> { _uiState.update { it.copy(isOperating = false, error = result.message) }; loadDeviceStatus() }
                is NetworkResult.Exception -> { _uiState.update { it.copy(isOperating = false, error = result.e.localizedMessage) } }
                else -> _uiState.update { it.copy(isOperating = false) }
            }
        }
    }
}
