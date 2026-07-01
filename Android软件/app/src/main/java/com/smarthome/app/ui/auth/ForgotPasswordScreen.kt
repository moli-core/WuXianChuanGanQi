package com.smarthome.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(onNavigateBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("找回密码") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (sent) {
                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("重置密码链接已发送", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("请检查您的邮箱 ($email) 中的重置链接", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onNavigateBack) { Text("返回登录") }
            } else {
                Text("找回密码", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("输入注册时使用的邮箱，我们将发送重置密码链接", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("邮箱地址") }, leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { if (email.isNotBlank()) sent = true }, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = email.isNotBlank()) {
                    Text("发送重置链接")
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onNavigateBack) { Text("返回登录") }
            }
        }
    }
}
