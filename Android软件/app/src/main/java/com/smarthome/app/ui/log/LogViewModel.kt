package com.smarthome.app.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.core.model.AlertLogItem
import com.smarthome.app.core.model.OperationLogItem
import com.smarthome.app.core.model.VoiceLogItem
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.repository.LogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LogUiState(
    val selectedTab: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val voiceLogs: List<VoiceLogItem> = emptyList(),
    val operationLogs: List<OperationLogItem> = emptyList(),
    val alertLogs: List<AlertLogItem> = emptyList()
)

class LogViewModel(
    private val logRepository: LogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    init { loadAll() }

    fun onTabSelected(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val voiceResult = logRepository.getVoiceLogs()
            val operationResult = logRepository.getOperationLogs()
            val alertResult = logRepository.getAlertLogs()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    voiceLogs = (voiceResult as? NetworkResult.Success)?.data ?: emptyList(),
                    operationLogs = (operationResult as? NetworkResult.Success)?.data ?: emptyList(),
                    alertLogs = (alertResult as? NetworkResult.Success)?.data ?: emptyList(),
                    error = when {
                        voiceResult is NetworkResult.Error -> voiceResult.message
                        operationResult is NetworkResult.Error -> operationResult.message
                        alertResult is NetworkResult.Error -> alertResult.message
                        else -> null
                    }
                )
            }
        }
    }

    fun handleAlert(id: Long) {
        viewModelScope.launch {
            when (val result = logRepository.handleAlert(id)) {
                is NetworkResult.Success -> loadAll()
                else -> {}
            }
        }
    }
}
