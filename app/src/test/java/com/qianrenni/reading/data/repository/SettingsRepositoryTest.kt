package com.qianrenni.reading.data.repository

import androidx.compose.ui.text.font.FontFamily
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.qianrenni.reading.data.model.ReadFontFamily
import com.qianrenni.reading.data.model.ReadSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
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
        val settings = newRepo().readSettings().first()

        assertEquals(18f, settings.fontSize)
        assertEquals(40f, settings.lineHeight)
        assertEquals(2f, settings.letterSpacing)
        assertEquals(FontFamily.Default, settings.fontFamily)
    }

    @Test
    fun `updateSettings persists typography and font family`() = runTest {
        val repo = newRepo()

        repo.updateSettings(
            ReadSettings(
                fontSize = 30f,
                lineHeight = 50f,
                letterSpacing = 3f,
                fontFamily = ReadFontFamily.Serif.value
            )
        )

        val settings = repo.readSettings().first()
        assertEquals(30f, settings.fontSize)
        assertEquals(50f, settings.lineHeight)
        assertEquals(3f, settings.letterSpacing)
        assertEquals(ReadFontFamily.Serif.value, settings.fontFamily)
    }
}
