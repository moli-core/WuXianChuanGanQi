package com.smarthome.app.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.app.core.model.MessageType
import com.smarthome.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(viewModel: AiChatViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.size - 1)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("AI智能助手") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = uiState.inputText, onValueChange = viewModel::onInputChanged, placeholder = { Text("输入指令或问题...") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(24.dp), enabled = !uiState.isLoading)
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(onClick = { viewModel.sendMessage() }, enabled = uiState.inputText.isNotBlank() && !uiState.isLoading) {
                        Icon(Icons.Default.Send, contentDescription = "发送")
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.messages.isEmpty()) {
                // 快捷提示
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("我可以帮你：", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("💡 打开/关闭 红灯、绿灯、黄灯、蜂鸣器", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("🌤 查询城市天气（如：北京天气）", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("💬 智能对话", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                    items(uiState.messages) { msg ->
                        when (msg.type) {
                            MessageType.DEVICE_ACTION -> ActionResultCard(msg.content)
                            MessageType.WEATHER -> WeatherCard(msg.content)
                            else -> ChatBubble(content = msg.content, isUser = msg.isUser)
                        }
                    }
                    if (uiState.isLoading) { item { ChatBubble(content = "正在思考...", isUser = false) } }
                }
            }
            if (uiState.error != null) {
                Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun ChatBubble(content: String, isUser: Boolean) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        Surface(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isUser) 16.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 16.dp), color = if (isUser) Blue40 else MaterialTheme.colorScheme.surfaceVariant) {
            Text(text = content, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ActionResultCard(content: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (content.startsWith("✅")) StatusGreen.copy(alpha = 0.1f) else StatusRed.copy(alpha = 0.1f)
            )
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (content.startsWith("✅")) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (content.startsWith("✅")) StatusGreen else StatusRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = content.removePrefix("✅ ").removePrefix("❌ "), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun WeatherCard(content: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Blue40.copy(alpha = 0.1f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.WbCloudy, contentDescription = null, tint = Blue40, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = content, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
