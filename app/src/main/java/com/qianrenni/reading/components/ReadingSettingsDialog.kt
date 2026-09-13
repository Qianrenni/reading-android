package com.qianrenni.reading.components

import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qianrenni.reading.data.model.ReadFontFamily
import com.qianrenni.reading.data.model.ReadSettings
import com.qianrenni.reading.data.repository.ThemeMode
import com.qianrenni.reading.ui.theme.readingBackground
import com.qianrenni.reading.ui.theme.readingPalette

@Composable
fun ReadingSettings(
    settings: ReadSettings,
    themeMode: ThemeMode,
    isSystemDark: Boolean,
    onThemeChange: (ThemeMode) -> Unit,
    onSettingsChange: (ReadSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Text(
            "[主题即时生效；字号 / 行高 / 字体重进阅读页面生效]",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            )
        )
        // 主题与全应用（含个人中心）共用同一个 ThemeMode，切换后整 App 同步换肤
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(ThemeMode.entries) { mode ->
                ThemeSwatch(
                    mode = mode,
                    isSystemDark = isSystemDark,
                    selected = mode == themeMode,
                    onClick = { onThemeChange(mode) }
                )
            }
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

/**
 * 主题色块：圆内直接渲染该主题的纸张质感与正文色，下方为名称；选中时高亮描边。
 */
@Composable
private fun ThemeSwatch(
    mode: ThemeMode,
    isSystemDark: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = readingPalette(mode, isSystemDark)
    Column(
        modifier = Modifier
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .readingBackground(palette)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "文", color = palette.text, fontSize = 14.sp)
        }
        Text(
            text = mode.displayName,
            fontSize = 11.sp,
            maxLines = 1,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}