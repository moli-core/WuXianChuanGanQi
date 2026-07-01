package com.smarthome.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nickname: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(v: String) { _uiState.update { it.copy(username = v, error = null) } }
    fun onPasswordChanged(v: String) { _uiState.update { it.copy(password = v, error = null) } }
    fun onConfirmPasswordChanged(v: String) { _uiState.update { it.copy(confirmPassword = v, error = null) } }
    fun onNicknameChanged(v: String) { _uiState.update { it.copy(nickname = v, error = null) } }

    fun register() {
        val s = _uiState.value
        if (s.username.isBlank()) { _uiState.update { it.copy(error = "请输入用户名") }; return }
        if (s.password.isBlank()) { _uiState.update { it.copy(error = "请输入密码") }; return }
        if (s.password != s.confirmPassword) { _uiState.update { it.copy(error = "两次密码输入不一致") }; return }
        if (s.password.length < 6) { _uiState.update { it.copy(error = "密码长度至少6位") }; return }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.register(s.username, s.password, s.nickname.ifBlank { null })) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false, error = "网络连接失败: ${result.e.localizedMessage}") }
                }
                else -> {}
            }
        }
    }
}
