package com.qianrenni.reading.data.repository

import com.qianrenni.reading.InMemoryKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeRepositoryTest {

    @Test
    fun `defaults to SYSTEM when empty`() {
        val repo = ThemeRepositoryImpl(InMemoryKeyValueStore())

        assertEquals(ThemeMode.SYSTEM, repo.mode.value)
    }

    @Test
    fun `reads persisted theme mode`() {
        val store = InMemoryKeyValueStore()
        store.putString("theme_mode", ThemeMode.DARK.name)

        val repo = ThemeRepositoryImpl(store)

        assertEquals(ThemeMode.DARK, repo.mode.value)
    }

    @Test
    fun `unknown persisted value falls back to SYSTEM`() {
        val store = InMemoryKeyValueStore()
        store.putString("theme_mode", "NOT_A_MODE")

        val repo = ThemeRepositoryImpl(store)

        assertEquals(ThemeMode.SYSTEM, repo.mode.value)
    }

    @Test
    fun `setMode persists and updates state`() {
        val store = InMemoryKeyValueStore()
        val repo = ThemeRepositoryImpl(store)

        repo.setMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, repo.mode.value)
        assertEquals(ThemeMode.LIGHT.name, store.getString("theme_mode"))
    }

    @Test
    fun `all modes have display names`() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(true, mode.displayName.isNotBlank())
        }
    }

    @Test
    fun `preset five themes plus system mode`() {
        assertEquals(
            listOf("跟随系统", "白天", "黑夜", "绿色护眼", "羊皮纸", "木制家具"),
            ThemeMode.entries.map { it.displayName }
        )
    }

    @Test
    fun `isDark follows system only for SYSTEM mode`() {
        assertEquals(true, ThemeMode.SYSTEM.isDark(systemDark = true))
        assertEquals(false, ThemeMode.SYSTEM.isDark(systemDark = false))

        assertEquals(false, ThemeMode.LIGHT.isDark(systemDark = true))
        assertEquals(true, ThemeMode.DARK.isDark(systemDark = false))
        assertEquals(false, ThemeMode.GREEN.isDark(systemDark = true))
        assertEquals(false, ThemeMode.PARCHMENT.isDark(systemDark = true))
        assertEquals(true, ThemeMode.WOOD.isDark(systemDark = false))
    }

    @Test
    fun `persists every preset theme`() {
        ThemeMode.entries.forEach { mode ->
            val store = InMemoryKeyValueStore()
            val repo = ThemeRepositoryImpl(store)

            repo.setMode(mode)

            assertEquals(mode, repo.mode.value)
            assertEquals(mode, ThemeRepositoryImpl(store).mode.value)
        }
    }
}
