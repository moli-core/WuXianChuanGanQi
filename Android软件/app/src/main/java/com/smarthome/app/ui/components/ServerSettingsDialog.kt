package com.smarthome.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ServerSettingsDialog(
    currentHost: String,
    currentPort: Int,
    onSave: (host: String, port: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var host by remember { mutableStateOf(currentHost) }
    var port by remember { mutableStateOf(currentPort.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("服务器设置") },
        text = {
            Column {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it; error = null },
                    label = { Text("服务器地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it; error = null },
                    label = { Text("端口号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (host.isBlank()) {
                    error = "请输入服务器地址"
                    return@TextButton
                }
                val portNum = port.toIntOrNull()
                if (portNum == null || portNum !in 1..65535) {
                    error = "端口号必须在 1-65535 之间"
                    return@TextButton
                }
                onSave(host.trim(), portNum)
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
