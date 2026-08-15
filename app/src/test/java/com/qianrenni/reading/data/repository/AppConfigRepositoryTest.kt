package com.qianrenni.reading.data.repository

import com.qianrenni.reading.InMemoryKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Test

class AppConfigRepositoryTest {

    @Test
    fun `defaults to DEFAULT_BASE_URL when empty`() {
        val repo = AppConfigRepositoryImpl(InMemoryKeyValueStore())
        assertEquals(DEFAULT_BASE_URL, repo.currentBaseUrl())
        assertEquals(DEFAULT_BASE_URL, repo.baseUrl.value)
    }

    @Test
    fun `reads persisted base url`() {
        val store = InMemoryKeyValueStore()
        store.putString("base_url", "http://custom/")

        val repo = AppConfigRepositoryImpl(store)

        assertEquals("http://custom/", repo.currentBaseUrl())
        assertEquals("http://custom/", repo.baseUrl.value)
    }

    @Test
    fun `setBaseUrl normalizes and persists`() {
        val store = InMemoryKeyValueStore()
        val repo = AppConfigRepositoryImpl(store)

        repo.setBaseUrl(" http://new.example.com:8080/api ")

        assertEquals("http://new.example.com:8080/api/", repo.currentBaseUrl())
        assertEquals("http://new.example.com:8080/api/", store.getString("base_url"))
        assertEquals("http://new.example.com:8080/api/", repo.baseUrl.value)
    }

    @Test
    fun `setBaseUrl without trailing slash appends one`() {
        val repo = AppConfigRepositoryImpl(InMemoryKeyValueStore())
        repo.setBaseUrl("https://srv.example.com")
        assertEquals("https://srv.example.com/", repo.currentBaseUrl())
    }
}
