package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.User
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseHandlerTest {

    private fun client(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        contentType: String = "application/json"
    ): HttpClient = HttpClient(
        MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, contentType)) }
    ) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private suspend fun fetch(client: HttpClient, path: String = "test"): HttpResponse =
        client.get("http://test/$path")

    @Test
    fun `success json returns Success with parsed data`() = runBlocking {
        val json = """{"code":0,"message":"ok","data":{"id":7,"userName":"tom","email":"t@e.c","isActive":true}}"""
        val result = ResponseHandler.handleResponse<User>(fetch(client(json)))
        assertTrue(result is NetworkResult.Success)
        assertEquals(7, (result as NetworkResult.Success).data.id)
        assertEquals("tom", result.data.userName)
    }

    @Test
    fun `business failure returns Failure with message`() = runBlocking {
        val json = """{"code":1,"message":"用户名或密码错误","data":null}"""
        val result = ResponseHandler.handleResponse<User>(fetch(client(json)))
        assertTrue(result is NetworkResult.Failure)
        assertEquals("用户名或密码错误", (result as NetworkResult.Failure).message)
    }

    @Test
    fun `empty body 204 returns Empty`() = runBlocking {
        val result = ResponseHandler.handleResponse<Unit>(
            fetch(client("", status = HttpStatusCode.NoContent))
        )
        assertTrue(result is NetworkResult.Empty)
    }

    @Test
    fun `empty body 201 returns Empty`() = runBlocking {
        val result = ResponseHandler.handleResponse<Unit>(
            fetch(client("", status = HttpStatusCode.Created))
        )
        assertTrue(result is NetworkResult.Empty)
    }

    @Test
    fun `401 triggers onUnauthorized and returns Failure`() = runBlocking {
        var unauthorizedCalled = false
        val result = ResponseHandler.handleResponse<User>(
            fetch(client("", status = HttpStatusCode.Unauthorized)),
            onUnauthorized = { unauthorizedCalled = true }
        )
        assertTrue(unauthorizedCalled)
        assertTrue(result is NetworkResult.Failure)
        assertEquals(401, (result as NetworkResult.Failure).code)
    }

    @Test
    fun `non json 2xx returns failure with special message`() = runBlocking {
        val result = ResponseHandler.handleResponse<User>(
            fetch(client("hello", contentType = "text/plain"))
        )
        assertTrue(result is NetworkResult.Failure)
        assertEquals("非JSON响应但状态成功", (result as NetworkResult.Failure).message)
    }

    @Test
    fun `non json 500 returns generic failure`() = runBlocking {
        val result = ResponseHandler.handleResponse<User>(
            fetch(client("boom", contentType = "text/plain", status = HttpStatusCode.InternalServerError))
        )
        assertTrue(result is NetworkResult.Failure)
        assertEquals("请求失败", (result as NetworkResult.Failure).message)
    }

    @Test
    fun `isHttpSuccess recognizes 2xx range`() {
        assertTrue(ResponseHandler.isHttpSuccess(200))
        assertTrue(ResponseHandler.isHttpSuccess(204))
        assertTrue(!ResponseHandler.isHttpSuccess(401))
        assertTrue(!ResponseHandler.isHttpSuccess(500))
    }

    @Test
    fun `invalid json returns parse failure`() = runBlocking {
        val result = ResponseHandler.handleResponse<User>(
            fetch(client("""{"code":0,"data":"not an object"}"""))
        )
        assertTrue(result is NetworkResult.Failure)
        assertNull((result as NetworkResult.Failure).code)
    }
}
