package com.smarthome.app.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.core.model.WeatherData
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val weather: WeatherData? = null,
    val cityInput: String = ""
)

class WeatherViewModel(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    fun onCityChanged(city: String) {
        _uiState.update { it.copy(cityInput = city) }
    }

    fun loadWeather() {
        val city = _uiState.value.cityInput.trim().ifBlank { null }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = weatherRepository.getCurrentWeather(city)) {
                is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, weather = result.data) }
                is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.Exception -> _uiState.update { it.copy(isLoading = false, error = result.e.localizedMessage) }
                else -> {}
            }
        }
    }
}
