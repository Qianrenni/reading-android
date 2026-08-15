package com.qianrenni.reading.data.repository

import com.qianrenni.reading.InMemoryKeyValueStore
import com.qianrenni.reading.testUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SessionManagerTest {

    @Test
    fun `setToken persists to store`() {
        val store = InMemoryKeyValueStore()
        val session = SessionManager(store)

        session.setToken("a", "r", "Bearer")

        assertEquals("a", store.getString("access_token"))
        assertEquals("r", store.getString("refresh_token"))
        assertEquals("Bearer", store.getString("token_type"))
    }

    @Test
    fun `setToken with isSave false keeps memory but skips storage`() {
        val store = InMemoryKeyValueStore()
        val session = SessionManager(store)

        session.setToken("a", "r", "Bearer", isSave = false)

        assertNull(store.getString("access_token"))
        assertEquals("a", session.tokens()?.accessToken)
    }

    @Test
    fun `bearerTokens returns null when not logged in`() {
        val session = SessionManager(InMemoryKeyValueStore())
        assertNull(session.bearerTokens())
    }

    @Test
    fun `bearerTokens returns tokens after setToken`() {
        val session = SessionManager(InMemoryKeyValueStore())
        session.setToken("a", "r", "Bearer", isSave = false)

        val tokens = session.bearerTokens()

        assertNotNull(tokens)
        assertEquals("a", tokens?.accessToken)
        assertEquals("r", tokens?.refreshToken)
    }

    @Test
    fun `tokens returns SavedTokens from memory`() {
        val session = SessionManager(InMemoryKeyValueStore())
        session.setToken("a", "r", "Bearer", isSave = false)

        val tokens = session.tokens()

        assertEquals("a", tokens?.accessToken)
        assertEquals("r", tokens?.refreshToken)
        assertEquals("Bearer", tokens?.tokenType)
    }

    @Test
    fun `savedTokens reads persisted tokens`() {
        val store = InMemoryKeyValueStore()
        val session = SessionManager(store)
        session.setToken("a", "r", "Bearer")

        val saved = session.savedTokens()

        assertEquals("a", saved?.accessToken)
        assertEquals("r", saved?.refreshToken)
        assertEquals("Bearer", saved?.tokenType)
    }

    @Test
    fun `savedTokens returns null when empty`() {
        val session = SessionManager(InMemoryKeyValueStore())
        assertNull(session.savedTokens())
    }

    @Test
    fun `setUser updates user flow`() {
        val session = SessionManager(InMemoryKeyValueStore())
        session.setUser(testUser(id = 7))
        assertEquals(7, session.user.value?.id)

        session.setUser(null)
        assertNull(session.user.value)
    }

    @Test
    fun `clear resets memory and storage`() {
        val store = InMemoryKeyValueStore()
        val session = SessionManager(store)
        session.setToken("a", "r", "Bearer")
        session.setUser(testUser())

        session.clear()

        assertNull(session.tokens())
        assertNull(session.user.value)
        assertNull(store.getString("access_token"))
        assertNull(store.getString("refresh_token"))
        assertNull(store.getString("token_type"))
    }
}
