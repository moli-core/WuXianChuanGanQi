package com.smarthome.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onRegisterSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("注册") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("创建新账号", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(value = uiState.username, onValueChange = viewModel::onUsernameChanged, label = { Text("用户名") }, leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(value = uiState.nickname, onValueChange = viewModel::onNicknameChanged, label = { Text("昵称（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(value = uiState.password, onValueChange = viewModel::onPasswordChanged, label = { Text("密码") }, leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(value = uiState.confirmPassword, onValueChange = viewModel::onConfirmPasswordChanged, label = { Text("确认密码") }, leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading)

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { viewModel.register() }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !uiState.isLoading) {
                if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text("注册", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
