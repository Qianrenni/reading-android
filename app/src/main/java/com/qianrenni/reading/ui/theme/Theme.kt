package com.qianrenni.reading.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.qianrenni.reading.data.repository.ThemeMode

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

/** 绿色护眼：柔和绿纸底 + 深墨绿正文。 */
private val GreenColorScheme = lightColorScheme(
    primary = Color(0xFF2F6B3A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7E3BF),
    onPrimaryContainer = Color(0xFF08210E),

    secondary = Color(0xFF4F6350),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E8D3),
    onSecondaryContainer = Color(0xFF0D1F10),

    tertiary = Color(0xFF3B6558),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBEEBDC),
    onTertiaryContainer = Color(0xFF002018),

    // 经典护眼底色 / 正文深墨绿，与阅读页纸张色保持一致
    background = Color(0xFFC7EDCC),
    onBackground = Color(0xFF2D4A2D),
    surface = Color(0xFFDFF2E1),
    surfaceContainer = Color(0xFFEAF7EC),
    onSurface = Color(0xFF2D4A2D),
    surfaceVariant = Color(0xFFC3DDC6),
    onSurfaceVariant = Color(0xFF455948),
    outline = Color(0xFF728573),
    outlineVariant = Color(0xFFC1D6C3),
    scrim = Color.Black.copy(alpha = 0.5f),
    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF)
)

/** 羊皮纸：米黄纸张质感，偏暖的旧纸配色。 */
private val ParchmentColorScheme = lightColorScheme(
    primary = Color(0xFF8A6B45),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEEDFC4),
    onPrimaryContainer = Color(0xFF2E2114),

    secondary = Color(0xFF6E5B45),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0E2CE),
    onSecondaryContainer = Color(0xFF241A10),

    tertiary = Color(0xFF96703E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6DFBE),
    onTertiaryContainer = Color(0xFF2F1F00),

    background = Color(0xFFF5F0E1),
    onBackground = Color(0xFF5B4636),
    surface = Color(0xFFFBF7EC),
    surfaceContainer = Color(0xFFFFFCF4),
    onSurface = Color(0xFF5B4636),
    surfaceVariant = Color(0xFFE7DCC5),
    onSurfaceVariant = Color(0xFF7A6350),
    outline = Color(0xFF9C8A72),
    outlineVariant = Color(0xFFDBCCB4),
    scrim = Color.Black.copy(alpha = 0.5f),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

/** 木制家具：深木色 + 琥珀色点缀，正文用暖白色。 */
private val WoodColorScheme = darkColorScheme(
    primary = Color(0xFFD8A76A),
    onPrimary = Color(0xFF3A2612),
    primaryContainer = Color(0xFF5A3F22),
    onPrimaryContainer = Color(0xFFF7E3C8),

    secondary = Color(0xFFC7A98A),
    onSecondary = Color(0xFF3A2B1B),
    secondaryContainer = Color(0xFF4C3A26),
    onSecondaryContainer = Color(0xFFF2E3CE),

    tertiary = Color(0xFFB98A5E),
    onTertiary = Color(0xFF3A2410),
    tertiaryContainer = Color(0xFF543619),
    onTertiaryContainer = Color(0xFFFFDDB8),

    background = Color(0xFF33241A),
    onBackground = Color(0xFFF2E6D4),
    surface = Color(0xFF3D2C20),
    surfaceContainer = Color(0xFF463326),
    onSurface = Color(0xFFF2E6D4),
    surfaceVariant = Color(0xFF55402E),
    onSurfaceVariant = Color(0xFFD8C2A6),
    outline = Color(0xFF8A7255),
    outlineVariant = Color(0xFF5A462F),
    scrim = Color.White.copy(alpha = 0.2f),
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000)
)

/** 主题 → Material 配色（[ThemeMode.SYSTEM] 按系统深浅色在白天 / 黑夜之间切换）。 */
fun colorSchemeOf(themeMode: ThemeMode, isSystemDark: Boolean): ColorScheme = when (themeMode) {
    ThemeMode.SYSTEM -> if (isSystemDark) DarkColorScheme else LightColorScheme
    ThemeMode.LIGHT -> LightColorScheme
    ThemeMode.DARK -> DarkColorScheme
    ThemeMode.GREEN -> GreenColorScheme
    ThemeMode.PARCHMENT -> ParchmentColorScheme
    ThemeMode.WOOD -> WoodColorScheme
}

/**
 * 全应用统一主题入口：传入当前 [ThemeMode] 即可，阅读页与其余页面共用同一套配色。
 */
@Composable
fun ReadingTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    isSystemDark: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (themeMode.isDark(isSystemDark)) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        else -> colorSchemeOf(themeMode, isSystemDark)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}