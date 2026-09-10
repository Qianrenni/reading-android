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
    // Primary: 温暖的咖啡棕 #8C5A2B
    primary = Color(0xFF8C5A2B),
    onPrimary = Color(0xFFFFFFFF), // 白色文字

    // PrimaryContainer: 浅奶咖色 (用于 Tag 背景或次要按钮)
    primaryContainer = Color(0xFFF5DEC4),
    onPrimaryContainer = Color(0xFF3A2007), // 深棕文字

    // Secondary: 中性暖棕，不抢主色风头
    secondary = Color(0xFF6F5B45),
    onSecondary = Color(0xFFFFFFFF),

    secondaryContainer = Color(0xFFF3E3D2),
    onSecondaryContainer = Color(0xFF28180A),

    // Tertiary: 陶土色 (用于强调、通知、特殊标签)
    tertiary = Color(0xFF9A6039),
    onTertiary = Color(0xFFFFFFFF),

    tertiaryContainer = Color(0xFFFFDBC7),
    onTertiaryContainer = Color(0xFF3A1600),

    // --- 背景与表面 ---
    background = Color(0xFFFDF6EF), // 米白，比纯白更柔和
    onBackground = Color(0xFF201A15),

    surface = Color(0xFFFDF6EF), // 卡片背景
    onSurface = Color(0xFF201A15),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0DFCC),
    onSurfaceVariant = Color(0xFF52443A),
    outline = Color(0xFF857469),
    outlineVariant = Color(0xFFD7C2B0),
    scrim = Color.Black.copy(alpha = 0.5f),
    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF)
)
private val DarkColorScheme = darkColorScheme(
    // Primary: 提亮的驼棕色 #B38A5A
    primary = Color(0xFFB38A5A),
    onPrimary = Color(0xFF3A2007), // 深棕文字，因为主色变亮了

    // PrimaryContainer: 深咖啡棕
    primaryContainer = Color(0xFF5A3D1E),
    onPrimaryContainer = Color(0xFFF5DEC4),

    // Secondary: 浅暖棕
    secondary = Color(0xFFCDBCA8),
    onSecondary = Color(0xFF36281A),

    secondaryContainer = Color(0xFF4E3F2E),
    onSecondaryContainer = Color(0xFFF3E3D2),

    // Tertiary: 稍亮的陶土色，保持温暖感
    tertiary = Color(0xFFE0B89A),
    onTertiary = Color(0xFF4A2A12),

    tertiaryContainer = Color(0xFF673F22),
    onTertiaryContainer = Color(0xFFFFDBC7),

    // --- 背景与表面 ---
    background = Color(0xFF1A130D), // 深棕黑背景
    onBackground = Color(0xFFEDE0D4),

    surface = Color(0xFF1F1810), // 稍亮的卡片背景
    surfaceContainer = Color(0xFF241C14),
    onSurface = Color(0xFFEDE0D4),

    surfaceVariant = Color(0xFF3A2E24),
    onSurfaceVariant = Color(0xFFD8C2B0),
    outline = Color(0xFFA08D7D),
    outlineVariant = Color(0xFF4A3D32),
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