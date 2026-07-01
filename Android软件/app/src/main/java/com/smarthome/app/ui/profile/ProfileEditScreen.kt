package com.smarthome.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileEditViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.changePasswordMode) "修改密码" else "编辑资料") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (uiState.changePasswordMode) {
                // 密码修改模式
                OutlinedTextField(value = uiState.oldPassword, onValueChange = viewModel::onOldPasswordChanged, label = { Text("原密码") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = uiState.newPassword, onValueChange = viewModel::onNewPasswordChanged, label = { Text("新密码") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = uiState.confirmPassword, onValueChange = viewModel::onConfirmPasswordChanged, label = { Text("确认新密码") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading)
            } else {
                // 资料编辑模式
                OutlinedTextField(value = uiState.nickname, onValueChange = viewModel::onNicknameChanged, label = { Text("昵称") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = uiState.phone, onValueChange = viewModel::onPhoneChanged, label = { Text("手机号") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = uiState.email, onValueChange = viewModel::onEmailChanged, label = { Text("邮箱") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading)
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (uiState.success != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(uiState.success!!, color = com.smarthome.app.ui.theme.StatusGreen, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { if (uiState.changePasswordMode) viewModel.changePassword() else viewModel.saveProfile() }, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = !uiState.isLoading) {
                if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text(if (uiState.changePasswordMode) "修改密码" else "保存")
            }

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = viewModel::togglePasswordMode) {
                Text(if (uiState.changePasswordMode) "← 返回编辑资料" else "修改密码 →")
            }
        }
    }
}
