package com.smarthome.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.core.datastore.AppSettingsDataStore
import com.smarthome.app.core.datastore.TokenDataStore
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val nickname: String = "",
    val username: String = "",
    val role: String = "",
    val isLoading: Boolean = false,
    val isLoggingOut: Boolean = false,
    val serverHost: String = AppSettingsDataStore.DEFAULT_HOST,
    val serverPort: Int = AppSettingsDataStore.DEFAULT_PORT,
    val showSettingsDialog: Boolean = false
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val tokenDataStore: TokenDataStore,
    private val appSettingsDataStore: AppSettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val nickname = tokenDataStore.nickname.first() ?: ""
            val username = tokenDataStore.username.first() ?: ""
            val role = tokenDataStore.role.first() ?: ""
            val host = appSettingsDataStore.getBaseUrl()
            val port = appSettingsDataStore.getBasePort()
            _uiState.update { it.copy(isLoading = false, nickname = nickname, username = username, role = role, serverHost = host, serverPort = port) }

            when (val result = authRepository.getProfile()) {
                is NetworkResult.Success -> {
                    val user = result.data
                    _uiState.update { it.copy(nickname = user.nickname.ifEmpty { nickname }, username = user.username.ifEmpty { username }, role = user.role.ifEmpty { role }) }
                }
                else -> {}
            }
        }
    }

    fun showSettings() {
        _uiState.update { it.copy(showSettingsDialog = true) }
    }

    fun hideSettings() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    fun saveSettings(host: String, port: Int) {
        viewModelScope.launch {
            appSettingsDataStore.saveBaseUrl(host)
            appSettingsDataStore.saveBasePort(port)
            _uiState.update { it.copy(serverHost = host, serverPort = port, showSettingsDialog = false) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            authRepository.logout()
            _uiState.update { it.copy(isLoggingOut = false) }
        }
    }
}
