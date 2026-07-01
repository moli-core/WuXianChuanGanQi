package com.smarthome.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.core.datastore.TokenDataStore
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileEditUiState(
    val nickname: String = "",
    val phone: String = "",
    val email: String = "",
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val changePasswordMode: Boolean = false
)

class ProfileEditViewModel(
    private val authRepository: AuthRepository,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(nickname = tokenDataStore.nickname.first() ?: "") }
            when (val result = authRepository.getProfile()) {
                is NetworkResult.Success -> {
                    val u = result.data
                    _uiState.update { it.copy(nickname = u.nickname, phone = u.phone ?: "", email = u.email ?: "") }
                }
                else -> {}
            }
        }
    }

    fun onNicknameChanged(v: String) { _uiState.update { it.copy(nickname = v, error = null, success = null) } }
    fun onPhoneChanged(v: String) { _uiState.update { it.copy(phone = v, error = null, success = null) } }
    fun onEmailChanged(v: String) { _uiState.update { it.copy(email = v, error = null, success = null) } }
    fun onOldPasswordChanged(v: String) { _uiState.update { it.copy(oldPassword = v, error = null, success = null) } }
    fun onNewPasswordChanged(v: String) { _uiState.update { it.copy(newPassword = v, error = null, success = null) } }
    fun onConfirmPasswordChanged(v: String) { _uiState.update { it.copy(confirmPassword = v, error = null, success = null) } }
    fun togglePasswordMode() { _uiState.update { it.copy(changePasswordMode = !it.changePasswordMode, error = null, success = null) } }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, success = null) }
            when (val result = authRepository.updateProfile(
                _uiState.value.nickname,
                _uiState.value.phone.ifBlank { null },
                _uiState.value.email.ifBlank { null }
            )) {
                is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, success = "个人信息已更新") }
                is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.Exception -> _uiState.update { it.copy(isLoading = false, error = result.e.localizedMessage) }
                else -> {}
            }
        }
    }

    fun changePassword() {
        val s = _uiState.value
        if (s.oldPassword.isBlank()) { _uiState.update { it.copy(error = "请输入原密码") }; return }
        if (s.newPassword.length < 6) { _uiState.update { it.copy(error = "新密码至少6位") }; return }
        if (s.newPassword != s.confirmPassword) { _uiState.update { it.copy(error = "两次密码不一致") }; return }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, success = null) }
            when (val result = authRepository.changePassword(s.oldPassword, s.newPassword)) {
                is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, success = result.data, oldPassword = "", newPassword = "", confirmPassword = "") }
                is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.Exception -> _uiState.update { it.copy(isLoading = false, error = result.e.localizedMessage) }
                else -> {}
            }
        }
    }
}
