package com.qianrenni.reading.data.repository

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.qianrenni.reading.data.model.ReadSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SettingsRepositoryTest {

    private fun newRepo(): SettingsRepositoryImpl {
        val tmp = File.createTempFile("read_settings", ".preferences_pb")
        tmp.deleteOnExit()
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher()),
            produceFile = { tmp }
        )
        return SettingsRepositoryImpl(dataStore)
    }

    @Test
    fun `readSettings returns defaults when empty`() = runTest {
        val repo = newRepo()
        val colorScheme = lightColorScheme()

        val settings = repo.readSettings(colorScheme).first()

        assertEquals(18f, settings.fontSize)
        assertEquals(40f, settings.lineHeight)
        assertEquals(2f, settings.letterSpacing)
        assertEquals(colorScheme.background.toArgb(), settings.backgroundColor)
        assertEquals(colorScheme.onBackground.toArgb(), settings.textColor)
    }

    @Test
    fun `updateSettings persists values`() = runTest {
        val repo = newRepo()
        val colorScheme = lightColorScheme()

        repo.updateSettings(
            ReadSettings(
                fontSize = 30f,
                lineHeight = 50f,
                letterSpacing = 3f,
                textColor = 0xFF111111.toInt(),
                backgroundColor = 0xFF222222.toInt()
            )
        )

        val settings = repo.readSettings(colorScheme).first()
        assertEquals(30f, settings.fontSize)
        assertEquals(50f, settings.lineHeight)
        assertEquals(3f, settings.letterSpacing)
        assertEquals(0xFF111111.toInt(), settings.textColor)
        assertEquals(0xFF222222.toInt(), settings.backgroundColor)
    }
}
