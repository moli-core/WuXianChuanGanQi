package com.smarthome.app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.core.model.*
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val deviceStats: DeviceStats? = null,
    val eventTrends: EventTrends? = null,
    val eventDistribution: EventDistribution? = null,
    val deviceRanking: List<DeviceRanking> = emptyList()
)

class ReportViewModel(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun refresh() { loadAll() }

    private fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val statsResult = reportRepository.getDeviceStats()
            val trendsResult = reportRepository.getEventTrends()
            val distResult = reportRepository.getEventDistribution()
            val rankingResult = reportRepository.getDeviceRanking()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    deviceStats = (statsResult as? NetworkResult.Success)?.data,
                    eventTrends = (trendsResult as? NetworkResult.Success)?.data,
                    eventDistribution = (distResult as? NetworkResult.Success)?.data,
                    deviceRanking = (rankingResult as? NetworkResult.Success)?.data ?: emptyList(),
                    error = if (statsResult is NetworkResult.Error) statsResult.message else null
                )
            }
        }
    }
}
