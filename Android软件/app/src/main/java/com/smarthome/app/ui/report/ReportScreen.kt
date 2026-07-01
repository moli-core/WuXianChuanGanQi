package com.smarthome.app.ui.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.app.ui.components.ErrorMessage
import com.smarthome.app.ui.components.LoadingIndicator
import com.smarthome.app.ui.components.StatCard
import com.smarthome.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("数据报表") }, actions = { IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Default.Refresh, contentDescription = "刷新") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(padding))
            uiState.error != null -> ErrorMessage(message = uiState.error!!, onRetry = { viewModel.refresh() }, modifier = Modifier.padding(padding))
            else -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
                    uiState.deviceStats?.let { stats ->
                        Text("设备概览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard("总设备", "${stats.totalDevices}", Modifier.weight(1f))
                            StatCard("在线", "${stats.onlineDevices}", Modifier.weight(1f))
                            StatCard("语音", "${stats.todayVoiceCount}", Modifier.weight(1f))
                            StatCard("告警", "${stats.todayAlertCount}", Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    uiState.eventTrends?.let { trends ->
                        Text("事件趋势", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (trends.values.isNotEmpty()) SimpleLineChart(data = trends.values.map { it.toFloat() }, labels = trends.dates.takeLast(7), modifier = Modifier.fillMaxWidth().height(200.dp))
                                else Text("暂无趋势数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    uiState.eventDistribution?.let { dist ->
                        Text("事件分布", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (dist.names.isNotEmpty()) SimplePieChart(values = dist.values.map { it.toFloat() }, labels = dist.names, modifier = Modifier.fillMaxWidth().height(200.dp))
                                else Text("暂无分布数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    if (uiState.deviceRanking.isNotEmpty()) {
                        Text("设备活跃排行", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                uiState.deviceRanking.forEachIndexed { index, item ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${index + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (index < 3) StatusOrange else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(24.dp))
                                        Text(item.deviceName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        Text("${item.count} 次", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (index < uiState.deviceRanking.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun SimpleLineChart(data: List<Float>, labels: List<String>, modifier: Modifier = Modifier) {
    val lineColor = Blue40
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val maxVal = data.max().coerceAtLeast(1f)
        val minVal = data.min().coerceAtMost(0f)
        val range = (maxVal - minVal).coerceAtLeast(1f)
        val stepX = size.width / (data.size - 1).coerceAtLeast(1)
        val padding = 20f
        val path = Path()
        data.forEachIndexed { index, value ->
            val x = index * stepX; val y = size.height - padding - ((value - minVal) / range) * (size.height - 2 * padding)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 3f))
        data.forEachIndexed { index, value ->
            val x = index * stepX; val y = size.height - padding - ((value - minVal) / range) * (size.height - 2 * padding)
            drawCircle(color = lineColor, radius = 4f, center = Offset(x, y))
        }
    }
}

@Composable
private fun SimplePieChart(values: List<Float>, labels: List<String>, modifier: Modifier = Modifier) {
    val colors = listOf(Blue40, Teal40, Amber40, StatusRed, StatusGreen, StatusOrange, Blue60, androidx.compose.ui.graphics.Color(0xFF9C27B0), androidx.compose.ui.graphics.Color(0xFFE91E63))
    Canvas(modifier = modifier) {
        val total = values.sum().coerceAtLeast(1f)
        val diameter = minOf(size.width, size.height) * 0.6f
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        var startAngle = -90f
        values.forEachIndexed { index, value ->
            val sweepAngle = (value / total) * 360f
            drawArc(color = colors[index % colors.size], startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true, topLeft = topLeft, size = Size(diameter, diameter))
            startAngle += sweepAngle
        }
    }
}
