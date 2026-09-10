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
}
