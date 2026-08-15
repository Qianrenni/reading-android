package com.qianrenni.reading.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun BottomControlBar(
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onCatalogClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 上一章
        ControlButton(
            icon = Icons.AutoMirrored.Filled.ArrowLeft,
            label = "上一章",
            onClick = {
                onPreviousClick()
                onDismiss()
            }
        )

        // 下一章
        ControlButton(
            icon = Icons.AutoMirrored.Filled.ArrowRight,
            label = "下一章",
            onClick = {
                onNextClick()
                onDismiss()
            }
        )

        // 目录
        ControlButton(
            icon = Icons.AutoMirrored.Filled.List,
            label = "目录",
            onClick = {
                onCatalogClick()
            }
        )

        // 阅读设置
        ControlButton(
            icon = Icons.Default.Settings,
            label = "设置",
            onClick = {
                onSettingsClick()
            }
        )
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = if (enabled)
                LocalContentColor.current
            else
                LocalContentColor.current.copy(alpha = 0.38f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled)
                LocalContentColor.current
            else
                LocalContentColor.current.copy(alpha = 0.38f)
        )
    }
}
