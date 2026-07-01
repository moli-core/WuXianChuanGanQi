package com.smarthome.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.app.ui.components.ConfirmDialog
import com.smarthome.app.ui.components.ServerSettingsDialog
import com.smarthome.app.ui.theme.StatusRed
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToLogs: () -> Unit,
    onNavigateToWeather: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToAiotDevices: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoggingOut) { if (uiState.isLoggingOut) onLogout() }

    if (showLogoutDialog) {
        ConfirmDialog(title = "退出登录", message = "确定要退出登录吗？", confirmText = "退出", onConfirm = { showLogoutDialog = false; viewModel.logout() }, onDismiss = { showLogoutDialog = false })
    }

    if (uiState.showSettingsDialog) {
        ServerSettingsDialog(
            currentHost = uiState.serverHost,
            currentPort = uiState.serverPort,
            onSave = { host, port -> viewModel.saveSettings(host, port) },
            onDismiss = { viewModel.hideSettings() }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("我的") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            // 用户信息头像区（可点击编辑）
            Surface(
                modifier = Modifier.clickable { onNavigateToEditProfile() },
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Text(uiState.nickname.firstOrNull()?.toString() ?: "U", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(uiState.nickname.ifEmpty { "用户" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("@${uiState.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            ProfileMenuItem(Icons.Default.Person, "编辑资料", "修改昵称、手机号、密码", onNavigateToEditProfile)
            ProfileMenuItem(Icons.Default.History, "日志记录", "查看语音、操作、告警日志", onNavigateToLogs)
            ProfileMenuItem(Icons.Default.Cloud, "天气", "查看当前天气信息", onNavigateToWeather)
            ProfileMenuItem(Icons.Default.Mic, "语音控制", "语音控制智能设备", onNavigateToVoice)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            ProfileMenuItem(Icons.Default.Settings, "服务器设置", "${uiState.serverHost}:${uiState.serverPort}", viewModel::showSettings)
            ProfileMenuItem(Icons.Default.Info, "关于", "版本 1.0.0") { }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { showLogoutDialog = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = StatusRed)) {
                Icon(Icons.Default.Logout, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("退出登录")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) }, leadingContent = { Icon(icon, contentDescription = null) }, modifier = Modifier.fillMaxWidth().clickable { onClick() })
}
