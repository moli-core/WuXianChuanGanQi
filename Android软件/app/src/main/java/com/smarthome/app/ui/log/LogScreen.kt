package com.smarthome.app.ui.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.app.core.model.AlertLogItem
import com.smarthome.app.core.model.OperationLogItem
import com.smarthome.app.core.model.VoiceLogItem
import com.smarthome.app.ui.components.ErrorMessage
import com.smarthome.app.ui.components.LoadingIndicator
import com.smarthome.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(onNavigateBack: () -> Unit, viewModel: LogViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("语音", "操作", "告警")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日志记录") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } },
                actions = { IconButton(onClick = { viewModel.loadAll() }) { Icon(Icons.Default.Refresh, contentDescription = "刷新") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = uiState.selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = uiState.selectedTab == index, onClick = { viewModel.onTabSelected(index) }, text = { Text(title) })
                }
            }
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.error != null -> ErrorMessage(message = uiState.error!!, onRetry = { viewModel.loadAll() })
                else -> when (uiState.selectedTab) {
                    0 -> VoiceLogList(logs = uiState.voiceLogs)
                    1 -> OperationLogList(logs = uiState.operationLogs)
                    2 -> AlertLogList(logs = uiState.alertLogs, onHandleAlert = viewModel::handleAlert)
                }
            }
        }
    }
}

@Composable
private fun VoiceLogList(logs: List<VoiceLogItem>) {
    var detailItem by remember { mutableStateOf<VoiceLogItem?>(null) }

    detailItem?.let { item ->
        AlertDialog(
            onDismissRequest = { detailItem = null },
            title = { Text("语音日志详情") },
            text = {
                Column {
                    Text("原始文本：${item.rawText}")
                    item.semanticResult?.let { Spacer(Modifier.height(8.dp)); Text("语义：$it") }
                    item.deviceCommand?.let { Spacer(Modifier.height(8.dp)); Text("设备指令：$it") }
                    item.source?.let { Spacer(Modifier.height(8.dp)); Text("来源：$it") }
                    item.parseMethod?.let { Spacer(Modifier.height(8.dp)); Text("解析方式：$it") }
                    item.operator?.let { Spacer(Modifier.height(8.dp)); Text("操作人：$it") }
                    item.createTime?.let { Spacer(Modifier.height(8.dp)); Text("时间：$it") }
                }
            },
            confirmButton = { TextButton(onClick = { detailItem = null }) { Text("关闭") } }
        )
    }

    if (logs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无语音日志", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs) { log ->
                Card(Modifier.fillMaxWidth().clickable { detailItem = log }) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(log.rawText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                        if (log.isValid != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(if (log.isValid!!) "✅ 有效" else "❌ 无效", style = MaterialTheme.typography.labelSmall)
                        }
                        log.createTime?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationLogList(logs: List<OperationLogItem>) {
    var detailItem by remember { mutableStateOf<OperationLogItem?>(null) }

    detailItem?.let { item ->
        AlertDialog(
            onDismissRequest = { detailItem = null },
            title = { Text("操作日志详情") },
            text = {
                Column {
                    Text("设备：${item.deviceName ?: item.deviceCode ?: "未知"}")
                    item.action?.let { Spacer(Modifier.height(8.dp)); Text("操作：$it") }
                    item.source?.let { Spacer(Modifier.height(8.dp)); Text("来源：$it") }
                    item.operator?.let { Spacer(Modifier.height(8.dp)); Text("操作人：$it") }
                    item.remark?.let { Spacer(Modifier.height(8.dp)); Text("备注：$it") }
                    item.createTime?.let { Spacer(Modifier.height(8.dp)); Text("时间：$it") }
                }
            },
            confirmButton = { TextButton(onClick = { detailItem = null }) { Text("关闭") } }
        )
    }

    if (logs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无操作日志", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs) { log ->
                Card(Modifier.fillMaxWidth().clickable { detailItem = log }) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TouchApp, null, tint = Teal40, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("${log.deviceName ?: log.deviceCode ?: "未知设备"} - ${log.action ?: ""}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                        log.createTime?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertLogList(logs: List<AlertLogItem>, onHandleAlert: (Long) -> Unit) {
    var detailItem by remember { mutableStateOf<AlertLogItem?>(null) }

    detailItem?.let { item ->
        AlertDialog(
            onDismissRequest = { detailItem = null },
            title = { Text("告警日志详情") },
            text = {
                Column {
                    Text("类型：${item.alertType ?: "未知"}")
                    Text("级别：${item.alertLevel}")
                    item.content?.let { Spacer(Modifier.height(8.dp)); Text("内容：$it") }
                    item.envValue?.let { Spacer(Modifier.height(8.dp)); Text("环境值：$it") }
                    Text("状态：${if (item.isHandled) "已处理" else "未处理"}")
                    item.handledBy?.let { Spacer(Modifier.height(8.dp)); Text("处理人：$it") }
                    item.createTime?.let { Spacer(Modifier.height(8.dp)); Text("时间：$it") }
                }
            },
            confirmButton = { TextButton(onClick = { detailItem = null }) { Text("关闭") } }
        )
    }

    if (logs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无告警日志", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { detailItem = log },
                    colors = CardDefaults.cardColors(containerColor = if (log.isHandled) MaterialTheme.colorScheme.surfaceVariant else StatusRed.copy(alpha = 0.05f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = if (log.isHandled) StatusGreen else StatusRed, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(log.alertType ?: "未知告警", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("级别: ${log.alertLevel}", style = MaterialTheme.typography.bodySmall, color = when (log.alertLevel) { 3 -> StatusRed; 2 -> StatusOrange; else -> StatusYellow })
                            }
                            if (!log.isHandled) {
                                TextButton(onClick = { onHandleAlert(log.id) }) { Text("标记处理", style = MaterialTheme.typography.labelMedium) }
                            } else {
                                Text("已处理", style = MaterialTheme.typography.labelMedium, color = StatusGreen)
                            }
                        }
                        log.content?.let { Spacer(Modifier.height(4.dp)); Text(it, style = MaterialTheme.typography.bodySmall) }
                        log.createTime?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}
