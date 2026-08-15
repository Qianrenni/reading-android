package com.qianrenni.reading.data.repository

import com.qianrenni.reading.FakeAuthApi
import com.qianrenni.reading.FakeTokenRefresher
import com.qianrenni.reading.InMemoryKeyValueStore
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.testLoginResponse
import com.qianrenni.reading.testUser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthRepositoryTest {

    @Test
    fun `initial with no saved tokens is no-op`() = runTest {
        val session = SessionManager(InMemoryKeyValueStore())
        val api = FakeAuthApi().apply { currentUserResult = NetworkResult.Success(testUser()) }
        val refresher = FakeTokenRefresher(testLoginResponse())

        AuthRepositoryImpl(session, api, refresher).initial()

        assertNull(session.user.value)
        assertEquals(0, refresher.calls)
    }

    @Test
    fun `initial restores user with valid access token`() = runTest {
        val session = SessionManager(InMemoryKeyValueStore())
        session.setToken("valid", "r", "Bearer")
        val api = FakeAuthApi().apply { currentUserResult = NetworkResult.Success(testUser(id = 7)) }
        val refresher = FakeTokenRefresher()

        AuthRepositoryImpl(session, api, refresher).initial()

        assertEquals(7, session.user.value?.id)
        assertEquals(0, refresher.calls)
    }

    @Test
    fun `initial refreshes when access token invalid`() = runTest {
        val session = SessionManager(InMemoryKeyValueStore())
        session.setToken("expired", "refreshMe", "Bearer")
        val api = FakeAuthApi().apply { currentUserResult = NetworkResult.Failure("401", 401) }
        val refresher = FakeTokenRefresher(
            testLoginResponse(access = "newAccess", refresh = "newRefresh", user = testUser(id = 9))
        )

        AuthRepositoryImpl(session, api, refresher).initial()

        assertEquals(1, refresher.calls)
        assertEquals("refreshMe", refresher.lastRefreshToken)
        assertEquals(9, session.user.value?.id)
        assertEquals("newAccess", session.tokens()?.accessToken)
        assertEquals("newRefresh", session.tokens()?.refreshToken)
    }

    @Test
    fun `initial leaves logged out when refresh fails`() = runTest {
        val session = SessionManager(InMemoryKeyValueStore())
        session.setToken("expired", "r", "Bearer")
        val api = FakeAuthApi().apply { currentUserResult = NetworkResult.Failure("401", 401) }
        val refresher = FakeTokenRefresher(null)

        AuthRepositoryImpl(session, api, refresher).initial()

        assertNull(session.user.value)
    }

    @Test
    fun `setUser and clear delegate to session`() {
        val session = SessionManager(InMemoryKeyValueStore())
        val repo = AuthRepositoryImpl(session, FakeAuthApi(), FakeTokenRefresher())

        repo.setUser(testUser())
        assertEquals(1, session.user.value?.id)

        repo.clear()
        assertNull(session.user.value)
    }
}
