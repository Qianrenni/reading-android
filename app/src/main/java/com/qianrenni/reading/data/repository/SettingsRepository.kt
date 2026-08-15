package com.qianrenni.reading.data.repository

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.qianrenni.reading.data.model.ReadFontFamily
import com.qianrenni.reading.data.model.ReadSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 阅读设置仓库（DataStore 持久化）。
 */
interface SettingsRepository {
    fun readSettings(colorScheme: ColorScheme): Flow<ReadSettings>
    suspend fun updateSettings(settings: ReadSettings)
}

/**
 * DataStore 实现。注入 [DataStore] 以便单元测试使用临时文件或内存实现。
 */
class SettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    private object PreferencesKeys {
        val FONT_SIZE = floatPreferencesKey("font_size")
        val LINE_HEIGHT = floatPreferencesKey("line_height")
        val LETTER_SPACING = floatPreferencesKey("letter_spacing")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val TEXT_COLOR = intPreferencesKey("text_color")
        val BACKGROUND_COLOR = intPreferencesKey("background_color")
    }

    override fun readSettings(colorScheme: ColorScheme): Flow<ReadSettings> {
        return dataStore.data.map { preferences ->
            ReadSettings(
                fontSize = preferences[PreferencesKeys.FONT_SIZE] ?: 18f,
                lineHeight = preferences[PreferencesKeys.LINE_HEIGHT] ?: 40f,
                letterSpacing = preferences[PreferencesKeys.LETTER_SPACING] ?: 2f,
                fontFamily = ReadFontFamily.entries.find { it.displayName == preferences[PreferencesKeys.FONT_FAMILY] }?.value
                    ?: FontFamily.Default,
                textColor = preferences[PreferencesKeys.TEXT_COLOR]
                    ?: colorScheme.onBackground.toArgb(),
                backgroundColor = preferences[PreferencesKeys.BACKGROUND_COLOR]
                    ?: colorScheme.background.toArgb()
            )
        }
    }

    override suspend fun updateSettings(settings: ReadSettings) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SIZE] = settings.fontSize
            preferences[PreferencesKeys.LINE_HEIGHT] = settings.lineHeight
            preferences[PreferencesKeys.LETTER_SPACING] = settings.letterSpacing
            preferences[PreferencesKeys.FONT_FAMILY] =
                ReadFontFamily.entries.find { it.value == settings.fontFamily }?.displayName ?: ""
            preferences[PreferencesKeys.TEXT_COLOR] = settings.textColor
            preferences[PreferencesKeys.BACKGROUND_COLOR] = settings.backgroundColor
        }
    }
}
