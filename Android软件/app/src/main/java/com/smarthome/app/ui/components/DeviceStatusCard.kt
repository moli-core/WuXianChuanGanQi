package com.smarthome.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.app.ui.theme.StatusGreen
import com.smarthome.app.ui.theme.StatusRed

@Composable
fun DeviceStatusCard(
    deviceName: String,
    isOn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isOn) StatusGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isOn) StatusGreen else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isOn) StatusGreen else StatusRed)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = deviceName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = if (isOn) "已开启" else "已关闭",
                style = MaterialTheme.typography.bodySmall,
                color = if (isOn) StatusGreen else StatusRed
            )
        }
    }
}
