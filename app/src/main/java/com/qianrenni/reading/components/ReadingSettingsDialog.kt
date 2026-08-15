package com.qianrenni.reading.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qianrenni.reading.data.model.ReadFontFamily
import com.qianrenni.reading.data.model.ReadSettings
import com.qianrenni.reading.data.model.Themes

@Composable
fun ReadingSettings(
    settings: ReadSettings,
    onSettingsChange: (ReadSettings) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Text(
            "[设置完成后重进阅读页面即可]",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("主题")
            for (it in Themes.entries) {
                Box(
                    modifier = Modifier
                        .size(32.dp) // 设置圆形大小
                        .background(
                            color = Color(it.backgroundColor), // 内部填充色
                            shape = CircleShape         // 圆形形状
                        )
                        .border(
                            width = 2.dp,               // 边框宽度
                            color = Color(it.textColor),       // 边框颜色（即原 textColor）
                            shape = CircleShape         // 保持圆形
                        )
                        .clickable(onClick = {
                            onSettingsChange(
                                settings.copy(
                                    textColor = it.textColor,
                                    backgroundColor = it.backgroundColor
                                )
                            )
                        })
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp) // 设置圆形大小
                    .background(
                        color = MaterialTheme.colorScheme.background, // 内部填充色
                        shape = CircleShape         // 圆形形状
                    )
                    .border(
                        width = 2.dp,               // 边框宽度
                        color = MaterialTheme.colorScheme.onBackground,       // 边框颜色（即原 textColor）
                        shape = CircleShape         // 保持圆形
                    )
                    .clickable(onClick = {
                        onSettingsChange(
                            settings.copy(
                                textColor = colorScheme.onBackground.toArgb(),
                                backgroundColor = colorScheme.background.toArgb()
                            )
                        )
                    })
            )

        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("字体大小")
            Slider(
                value = settings.fontSize,
                onValueChange = {
                    onSettingsChange(
                        settings.copy(
                            fontSize = it,
                            lineHeight = settings.lineHeight + it - settings.fontSize
                        )
                    )
                },
                valueRange = 12f..28f
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("行高")
            Slider(
                value = settings.lineHeight,
                onValueChange = {
                    onSettingsChange(settings.copy(lineHeight = it))
                },
                valueRange = settings.fontSize..settings.fontSize * 2
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("字间距")
            Slider(
                value = settings.letterSpacing,
                onValueChange = {
                    onSettingsChange(settings.copy(letterSpacing = it))
                },
                valueRange = 0.5f..4f
            )

        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(ReadFontFamily.entries) { fontFamily ->
                Row(
                    Modifier
                        .selectable(
                            selected = (fontFamily.value == settings.fontFamily),
                            onClick = { onSettingsChange(settings.copy(fontFamily = fontFamily.value)) },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = fontFamily.value == settings.fontFamily,
                        onClick = null
                    )
                    Text(
                        text = fontFamily.displayName,
                    )
                }
            }
        }
    }
}