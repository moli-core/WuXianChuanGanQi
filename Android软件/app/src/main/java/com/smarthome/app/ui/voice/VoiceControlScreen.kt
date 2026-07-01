package com.smarthome.app.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.smarthome.app.ui.components.DoubaoCharacter
import com.smarthome.app.ui.components.Expression
import com.smarthome.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceControlScreen(
    onNavigateBack: () -> Unit,
    viewModel: VoiceViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    ) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) viewModel.startListening(context)
    }

    // 按钮呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "pulse"
    )

    // 初始化SDK（DEMO用默认值，正式环境需替换）
    LaunchedEffect(Unit) {
        viewModel.initSDK(
            context,
            appId = "58418379",
            apiKey = "403eeac6d421f7414c08a8289d9cbbd3",
            apiSecret = "Njk1MTlmOWM4ODU3YjdlNmNiYjdkZDlk"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语音控制", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.3f))

            // 豆包形象
            DoubaoCharacter(
                size = 100.dp,
                expression = if (uiState.isListening) Expression.HAPPY else Expression.SMILE
            )

            Spacer(Modifier.height(24.dp))

            // 状态文字
            Text(
                text = when {
                    uiState.isListening -> "正在聆听..."
                    uiState.isProcessing -> "正在处理..."
                    else -> "点击按钮开始语音控制"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DoubaoOrange
            )

            Spacer(Modifier.height(8.dp))

            // 识别结果
            if (uiState.recognizedText.isNotEmpty()) {
                Text(uiState.recognizedText, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(Modifier.height(8.dp))

            // 执行结果
            if (uiState.resultMessage.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.resultMessage.contains("✅") || uiState.resultMessage.contains("°C"))
                            StatusGreen.copy(alpha = 0.1f) else DoubaoOrangeBg
                    )
                ) {
                    Text(uiState.resultMessage, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }

            if (uiState.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(uiState.error!!, color = StatusRed, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.weight(0.5f))

            // 录音按钮
            Box(contentAlignment = Alignment.Center) {
                if (uiState.isListening) {
                    // 呼吸动画外圈
                    Box(
                        modifier = Modifier.size(140.dp).scale(scale)
                            .clip(CircleShape)
                            .background(DoubaoOrange.copy(alpha = 0.15f))
                    )
                }
                Button(
                    onClick = {
                        if (!hasPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else if (uiState.isListening) {
                            viewModel.stopListening()
                        } else {
                            viewModel.startListening(context)
                        }
                    },
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isListening) StatusRed else DoubaoOrange,
                        disabledContainerColor = Color.Gray
                    ),
                    enabled = uiState.isInitialized
                ) {
                    Icon(
                        if (uiState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (uiState.isListening) {
                Text("点击停止", style = MaterialTheme.typography.bodySmall, color = StatusRed)
            } else {
                Text("长按或点击开始录音", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(32.dp))

            // 指令提示
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DoubaoOrangeBg)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("试试说：", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("💡 \"打开红灯\"", style = MaterialTheme.typography.bodySmall)
                    Text("💡 \"关闭蜂鸣器\"", style = MaterialTheme.typography.bodySmall)
                    Text("💡 \"北京天气\"", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
