package com.smarthome.app.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.app.ui.components.ErrorMessage
import com.smarthome.app.ui.components.LoadingIndicator
import com.smarthome.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    onNavigateToAiotDevices: () -> Unit,
    viewModel: DeviceListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设备控制", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
        }
    ) { padding ->
        if (uiState.isLoading) LoadingIndicator(modifier = Modifier.padding(padding))
        else if (uiState.error != null) ErrorMessage(message = uiState.error!!, onRetry = { viewModel.loadDeviceStatus() }, modifier = Modifier.padding(padding))
        else {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                // 全部开关
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionBtn("全部开启", Icons.Default.Power, StatusGreen, { viewModel.allOn() }, Modifier.weight(1f))
                    ActionBtn("全部关闭", Icons.Default.PowerOff, StatusRed, { viewModel.allOff() }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(20.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.devices) { device ->
                        DeviceItem(
                            name = device.deviceName.ifEmpty { device.deviceCode },
                            isOn = device.status == 1,
                            onToggle = { viewModel.toggleDevice(device.deviceCode, device.status) },
                            enabled = !uiState.isOperating
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(name: String, isOn: Boolean, onToggle: () -> Unit, enabled: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isOn) DoubaoOrange.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOn) 0.dp else 1.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (isOn) DoubaoOrange.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isOn) Icons.Default.Lightbulb else Icons.Outlined.Lightbulb, null,
                    tint = if (isOn) DoubaoOrange else Color.Gray, modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(if (isOn) "已开启" else "已关闭", style = MaterialTheme.typography.bodySmall, color = if (isOn) StatusGreen else Color.Gray)
            }
            Switch(
                checked = isOn, onCheckedChange = { onToggle() }, enabled = enabled,
                colors = SwitchDefaults.colors(checkedTrackColor = DoubaoOrange, checkedThumbColor = Color.White)
            )
        }
    }
}

@Composable
private fun ActionBtn(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick, modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}
