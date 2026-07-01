package com.smarthome.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smarthome.app.ui.components.DoubaoCharacter
import com.smarthome.app.ui.components.Expression
import com.smarthome.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit = {},
    viewModel: LoginViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoginSuccess) { if (uiState.isLoginSuccess) onLoginSuccess() }

    Box(
        modifier = Modifier.fillMaxSize().background(LightBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 豆包形象
            DoubaoCharacter(size = 96.dp, expression = Expression.HAPPY)
            Spacer(Modifier.height(16.dp))
            Text("无线智能家居", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = DoubaoOrange)
            Text("SmartHome", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    OutlinedTextField(value = uiState.username, onValueChange = viewModel::onUsernameChanged, label = { Text("用户名") }, leadingIcon = { Icon(Icons.Default.Person, null, tint = DoubaoOrange) }, singleLine = true, shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = uiState.password, onValueChange = viewModel::onPasswordChanged, label = { Text("密码") }, leadingIcon = { Icon(Icons.Default.Lock, null, tint = DoubaoOrange) }, trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } }, singleLine = true, shape = RoundedCornerShape(16.dp), visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); viewModel.login() }), modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading)

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onNavigateToForgotPassword) { Text("忘记密码？", color = DoubaoOrange, style = MaterialTheme.typography.bodySmall) }
                    }

                    if (uiState.error != null) {
                        Text(uiState.error!!, color = StatusRed, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.login() }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !uiState.isLoading, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DoubaoOrange)) {
                        if (uiState.isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("登  录", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onNavigateToRegister) { Text("没有账号？点击注册", color = DoubaoOrange) }
        }
    }
}
