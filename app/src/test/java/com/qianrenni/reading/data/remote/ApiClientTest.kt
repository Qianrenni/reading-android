package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.User
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiClientTest {

    private val userJson =
        """{"code":0,"data":{"id":1,"userName":"a","email":"a@b.c","isActive":true}}"""

    @Test
    fun `get appends base url and parses success`() = runBlocking {
        var requestedUrl = ""
        val api = buildTestApiClient { req ->
            requestedUrl = req.url.toString()
            jsonBody(userJson)
        }
        val result = api.get<User>("user/me")
        assertTrue(result is NetworkResult.Success)
        assertEquals(TEST_BASE_URL + "user/me", requestedUrl)
        assertEquals(1, (result as NetworkResult.Success).data.id)
    }

    @Test
    fun `base url provider is evaluated per request`() = runBlocking {
        var base = "http://a/"
        var requestedUrl = ""
        val api = buildTestApiClient(baseUrlProvider = { base }) { req ->
            requestedUrl = req.url.toString()
            jsonBody(userJson)
        }
        api.get<User>("x")
        assertEquals("http://a/x", requestedUrl)

        base = "http://b/"
        api.get<User>("x")
        assertEquals("http://b/x", requestedUrl)
    }

    @Test
    fun `401 triggers onUnauthorized and returns failure`() = runBlocking {
        var unauthorized = false
        val api = buildTestApiClient(onUnauthorized = { unauthorized = true }) {
            jsonBody("", HttpStatusCode.Unauthorized)
        }
        val result = api.get<User>("x")
        assertTrue(unauthorized)
        assertTrue(result is NetworkResult.Failure)
    }

    @Test
    fun `network error returns failure`() = runBlocking {
        val api = buildTestApiClient { throw RuntimeException("boom") }
        val result = api.get<User>("x")
        assertTrue(result is NetworkResult.Failure)
    }

    @Test
    fun `post uses POST method and maps null data to empty`() = runBlocking {
        var method = ""
        val api = buildTestApiClient { req ->
            method = req.method.value
            jsonBody("""{"code":0,"data":null}""")
        }
        val result = api.post<Unit>("thing")
        assertEquals("POST", method)
        assertTrue(result is NetworkResult.Empty)
    }

    @Test
    fun `business failure maps to failure with message`() = runBlocking {
        val api = buildTestApiClient { jsonBody("""{"code":1,"message":"业务失败","data":null}""") }
        val result = api.delete<User>("thing/1")
        assertTrue(result is NetworkResult.Failure)
        assertEquals("业务失败", (result as NetworkResult.Failure).message)
    }
}
