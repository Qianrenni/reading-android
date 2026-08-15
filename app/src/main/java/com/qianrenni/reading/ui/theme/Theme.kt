package com.qianrenni.reading.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    // --- 核心品牌色 ---
    // Primary: 眼睛的清透蓝色 #4A90E2
    primary = Color(0xFFD5A13E),
    onPrimary = Color(0xFF0E181B), // 白色文字

    // PrimaryContainer: 浅天蓝 #D3E4FD (用于 Tag 背景或次要按钮)
    primaryContainer = Color(0xFFD5A13E).copy(alpha = 0.1f),
    onPrimaryContainer = Color(0xFFD5A13E), // 深蓝文字

    // Secondary: 中性灰，不抢主色风头
    secondary = Color(0xFFE0E0E0),
    onSecondary = Color(0xFF0E181B),

    secondaryContainer = Color(0xFFF5F5F5),
    onSecondaryContainer = Color(0xFF0E181B),

    // Tertiary: 脸颊的桃粉色 #FF8A65 (用于强调、通知、特殊标签)
    tertiary = Color(0xFFF7A595),
    onTertiary = Color(0xFF0E181B), // 黑色文字，因为粉色较亮

    tertiaryContainer = Color(0xFFF7A595).copy(alpha = 0.2f),
    onTertiaryContainer = Color(0xFFF7A595),

    // --- 背景与表面 ---
    background = Color(0xFFe0e0e0), // 极浅灰，比纯白更有质感
    onBackground = Color(0xFF0E181B),

    surface = Color(0xFFF0F0F0), // 纯白卡片
    onSurface = Color(0xFF0E181B),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF565656),
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFFBDBDBD),
    scrim = Color.Black.copy(alpha = 0.5f),
    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF)
)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD5A13E),
    onPrimary = Color(0xFF0E181B), // 深色文字，因为蓝色变亮了

    // PrimaryContainer: 深蓝色 #0D47A1
    primaryContainer = Color(0xFFD5A13E).copy(alpha = 0.2f),
    onPrimaryContainer = Color(0xFFD5A13E),

    // Secondary: 深灰
    secondary = Color(0xFF424242),
    onSecondary = Color(0xFFE0E0E0),

    secondaryContainer = Color(0xFF303030),
    onSecondaryContainer = Color(0xFFE0E0E0),

    // Tertiary: 稍暗的橙色 #FF7043，保持温暖感
    tertiary = Color(0xFFF7A595),
    onTertiary = Color(0xFF0E181B),

    tertiaryContainer = Color(0xFFF7A595).copy(alpha = 0.2f),
    onTertiaryContainer = Color(0xFFF7A595),

    // --- 背景与表面 ---
    background = Color(0xFF0E181B), // 标准暗黑背景
    onBackground = Color(0xFFE0E0E0),

    surface = Color(0xFF162429), // 稍亮的卡片背景
    surfaceContainer = Color(0xFF162429),
    onSurface = Color(0xFFE0E0E0),

    surfaceVariant = Color(0xFF2C383E),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF9E9E9E),
    outlineVariant = Color(0xFF424242),
    scrim = Color.White.copy(alpha = 0.2f),

    error = Color(0xFFCF6679),
    onError = Color(0xFF000000)
)

@Composable
fun ReadingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}