package com.qianrenni.reading.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TokenRefresherTest {

    private fun buildRefresher(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        onRequest: (HttpRequestData) -> Unit = {},
    ): KtorTokenRefresher {
        val engine = MockEngine { req ->
            onRequest(req)
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return KtorTokenRefresher(client, { TEST_BASE_URL })
    }

    @Test
    fun `refresh success returns login response and sends refresh token as bearer`() = runTest {
        var authHeader: String? = null
        val refresher = buildRefresher(
            """{"code":0,"data":{"accessToken":"na","refreshToken":"nr","tokenType":"Bearer","user":{"id":1,"userName":"a","email":"a@b.c","isActive":true}}}"""
        ) { req -> authHeader = req.headers["Authorization"] }

        val result = refresher.refresh("Bearer", "oldRefresh")

        assertNotNull(result)
        assertEquals("Bearer oldRefresh", authHeader)
        assertEquals("na", result?.accessToken)
        assertEquals("nr", result?.refreshToken)
    }

    @Test
    fun `refresh business failure returns null`() = runTest {
        val refresher = buildRefresher("""{"code":1,"message":"bad","data":null}""")
        assertNull(refresher.refresh("Bearer", "x"))
    }

    @Test
    fun `refresh network error returns null`() = runTest {
        val engine = MockEngine { throw RuntimeException("boom") }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val refresher = KtorTokenRefresher(client, { TEST_BASE_URL })
        assertNull(refresher.refresh("Bearer", "x"))
    }
}
