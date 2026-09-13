package com.qianrenni.reading

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qianrenni.reading.navigation.AppNavigation
import com.qianrenni.reading.ui.theme.ReadingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val themeRepository = (application as ReadingApplication).container.themeRepository
        setContent {
            val themeMode by themeRepository.mode.collectAsStateWithLifecycle()
            val isSystemDark = isSystemInDarkTheme()
            val darkTheme = themeMode.isDark(isSystemDark)
            // 手动切换主题时同步系统栏样式，避免状态栏图标与背景同色而看不清
            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = systemBarStyle(darkTheme),
                    navigationBarStyle = systemBarStyle(darkTheme)
                )
            }
            ReadingTheme(themeMode = themeMode, isSystemDark = isSystemDark) {
                AppNavigation()
            }
        }
    }
}

/** 深色主题用浅色图标，浅色主题用深色图标。 */
private fun systemBarStyle(darkTheme: Boolean): SystemBarStyle =
    if (darkTheme) {
        SystemBarStyle.dark(Color.TRANSPARENT)
    } else {
        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    }