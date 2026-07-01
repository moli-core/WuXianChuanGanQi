package com.smarthome.app.ui.weather

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.app.ui.components.ErrorMessage
import com.smarthome.app.ui.components.LoadingIndicator
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(onNavigateBack: () -> Unit, viewModel: WeatherViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("天气") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } })
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // 城市搜索栏
            OutlinedTextField(
                value = uiState.cityInput,
                onValueChange = { viewModel.onCityChanged(it) },
                label = { Text("输入城市名") },
                placeholder = { Text("例如：北京、上海、广州") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { viewModel.loadWeather() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "查询")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 天气内容
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.error != null -> ErrorMessage(message = uiState.error!!, onRetry = { viewModel.loadWeather() })
                uiState.weather != null -> {
                    val w = uiState.weather!!
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.WbCloudy, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(w.city ?: "", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${w.temperature ?: "--"}°C", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(w.weather ?: "", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(48.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            WeatherInfoItem(label = "湿度", value = w.humidity ?: "--")
                            WeatherInfoItem(label = "风向", value = w.windDirection ?: "--")
                            WeatherInfoItem(label = "风速", value = w.windSpeed ?: "--")
                        }
                        w.updateTime?.let { Spacer(modifier = Modifier.height(24.dp)); Text("更新于 $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("输入城市名称查询天气", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
