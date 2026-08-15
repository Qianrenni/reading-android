package com.qianrenni.reading.navigation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `data object routes are singletons and distinct`() {
        assertEquals(Home, Home)
        assertEquals(Login, Login)
        assertEquals(Register, Register)
        assertEquals(ForgetPassword, ForgetPassword)
        assertEquals(UpdatePassword, UpdatePassword)
        assertEquals(Bookshelf, Bookshelf)
        assertEquals(History, History)
        assertEquals(Profile, Profile)
        assertEquals(PrivacyPolicy, PrivacyPolicy)

        // 不同路由不等价
        org.junit.Assert.assertNotEquals(Home, Profile)
    }

    @Test
    fun `BookRead serializes with args`() {
        val encoded = json.encodeToString(BookRead.serializer(), BookRead(1, 2))
        assertEquals("""{"bookId":1,"chapterId":2}""", encoded)

        val decoded = json.decodeFromString(BookRead.serializer(), encoded)
        assertEquals(BookRead(1, 2), decoded)
    }

    @Test
    fun `BookInfo serializes with args`() {
        val encoded = json.encodeToString(BookInfo.serializer(), BookInfo(9))
        assertEquals("""{"bookId":9}""", encoded)

        val decoded = json.decodeFromString(BookInfo.serializer(), encoded)
        assertEquals(BookInfo(9), decoded)
    }
}
