package com.smarthome.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smarthome.app.ui.components.DoubaoCharacter
import com.smarthome.app.ui.components.EnvironmentCard
import com.smarthome.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToWeather: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToDevice: () -> Unit,
    onNavigateToChat: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能家居", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToVoice) { Icon(Icons.Default.Mic, contentDescription = "语音", tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = onNavigateToWeather) { Icon(Icons.Default.Cloud, contentDescription = "天气", tint = MaterialTheme.colorScheme.primary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (uiState.isLoading && !uiState.isRefreshing) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DoubaoOrange)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // === 豆包欢迎区域 ===
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DoubaoOrangeBg)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DoubaoCharacter(size = 64.dp, expression = com.smarthome.app.ui.components.Expression.HAPPY)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("☀️ 今天好！", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4A2800))
                            Spacer(Modifier.height(4.dp))
                            Text("室内温度 ${uiState.temperature}°C，湿度 ${uiState.humidity}%", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF7A5A3A))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // === 环境数据卡片 ===
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EnvCard(label = "🌡 温度", value = "${uiState.temperature}°", color = DoubaoCoral, modifier = Modifier.weight(1f))
                    EnvCard(label = "💧 湿度", value = "${uiState.humidity}%", color = Tertiary40, modifier = Modifier.weight(1f))
                    EnvCard(label = "🔥 烟雾", value = "${uiState.smokeLevel}", color = StatusOrange, modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))

                // === 快捷设备控制 ===
                Text("我的设备", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(12.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 第一行：绿灯 + 红灯（可直接点击开关）
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        DeviceToggleCard(
                            name = "绿灯", icon = Icons.Default.Lightbulb, isOn = uiState.deviceLed == 1,
                            onTap = { viewModel.toggleDevice("led", uiState.deviceLed) }, modifier = Modifier.weight(1f)
                        )
                        DeviceToggleCard(
                            name = "红灯", icon = Icons.Default.Lightbulb, isOn = uiState.deviceLedRed == 1,
                            onTap = { viewModel.toggleDevice("ledRed", uiState.deviceLedRed) }, modifier = Modifier.weight(1f)
                        )
                    }
                    // 第二行：黄灯 + 蜂鸣器
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        DeviceToggleCard(
                            name = "黄灯", icon = Icons.Default.Lightbulb, isOn = uiState.deviceLedYellow == 1,
                            onTap = { viewModel.toggleDevice("ledYellow", uiState.deviceLedYellow) }, modifier = Modifier.weight(1f)
                        )
                        DeviceToggleCard(
                            name = "蜂鸣器", icon = Icons.Default.Notifications, isOn = uiState.deviceBuzzer == 1,
                            onTap = { viewModel.toggleDevice("buzzer", uiState.deviceBuzzer) }, modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // === 人体红外检测 ===
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.devicePir == 1) StatusRed.copy(alpha = 0.08f) else StatusGreen.copy(alpha = 0.08f)
                    )
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        DoubaoCharacter(size = 40.dp, expression = if (uiState.devicePir == 1) com.smarthome.app.ui.components.Expression.WINK else com.smarthome.app.ui.components.Expression.SMILE)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("人体红外检测", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (uiState.devicePir == 1) "⚠️ 检测到人体闯入" else "✅ 安全，无人闯入",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.devicePir == 1) StatusRed else StatusGreen
                            )
                        }
                        Icon(
                            Icons.Default.Person, null,
                            tint = if (uiState.devicePir == 1) StatusRed else StatusGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // === 今日概览 ===
                Text("今日概览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCardDoubao(icon = Icons.Default.Warning, label = "告警", value = "${uiState.todayAlertCount}", color = StatusRed, modifier = Modifier.weight(1f))
                    StatCardDoubao(icon = Icons.Default.Mic, label = "语音指令", value = "${uiState.todayVoiceCount}", color = DoubaoOrange, modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(24.dp))

                // 快捷操作按钮
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton(icon = Icons.Default.Power, label = "设备控制", color = DoubaoOrange, onClick = onNavigateToDevice, modifier = Modifier.weight(1f))
                    ActionButton(icon = Icons.Default.SmartToy, label = "AI助手", color = Tertiary40, onClick = onNavigateToChat, modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(96.dp)) // bottom nav padding
            }
        }
    }
}

@Composable
private fun EnvCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun DeviceToggleCard(name: String, icon: ImageVector, isOn: Boolean, onTap: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOn) DoubaoOrange.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onTap
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isOn) DoubaoOrange.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (isOn) DoubaoOrange else Color.Gray, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium,
                color = if (isOn) DoubaoOrange else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (isOn) "已开启" else "已关闭", style = MaterialTheme.typography.labelSmall,
                color = if (isOn) StatusGreen else Color.Gray)
        }
    }
}

@Composable
private fun StatCardDoubao(icon: ImageVector, label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        border = null,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = color, fontWeight = FontWeight.Medium)
    }
}
