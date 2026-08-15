package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.EmailVerifyRequest
import com.qianrenni.reading.data.model.LoginRequest
import com.qianrenni.reading.data.model.RegisterRequest
import com.qianrenni.reading.data.model.User
import com.qianrenni.reading.data.model.UserRegister
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthApiTest {

    private val loginJson = """{"code":0,"data":{"accessToken":"at","refreshToken":"rt","tokenType":"Bearer","user":{"id":1,"userName":"tom","email":"t@e.c","isActive":true}}}"""
    private val userJson = """{"code":0,"data":{"id":1,"userName":"tom","email":"t@e.c","isActive":true}}"""

    @Test
    fun `getCaptcha returns image bytes and captures captcha id`() = runTest {
        val api = AuthApiImpl(buildTestApiClient { req ->
            assertEquals("/captcha/get", req.url.encodedPath)
            respond(byteArrayOf(1, 2, 3), HttpStatusCode.OK, headersOf("X-Captcha-Id", "abc123"))
        })

        val result = api.getCaptcha()
        assertTrue(result is NetworkResult.Success)
        assertEquals(3, (result as NetworkResult.Success).data.size)
    }

    @Test
    fun `login posts to token get with captcha id`() = runTest {
        var path = ""
        var captchaHeader: String? = null
        var method = ""
        val api = AuthApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            method = req.method.value
            captchaHeader = req.headers["X-Captcha-Id"]
            jsonBody(loginJson)
        })

        val result = api.login(LoginRequest("tom", "pw", "cap"), captchaId = "abc")

        assertEquals("/token/get", path)
        assertEquals("POST", method)
        assertEquals("abc", captchaHeader)
        assertTrue(result is NetworkResult.Success)
        assertEquals("at", (result as NetworkResult.Success).data.accessToken)
    }

    @Test
    fun `getCurrentUser parses user`() = runTest {
        var path = ""
        val api = AuthApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            jsonBody(userJson)
        })
        val result = api.getCurrentUser()
        assertEquals("/token/auth/me", path)
        assertTrue(result is NetworkResult.Success)
        assertEquals("tom", (result as NetworkResult.Success).data.userName)
    }

    @Test
    fun `register posts user and captcha`() = runTest {
        var path = ""
        var body = ""
        val api = AuthApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            body = req.bodyText()
            jsonBody("""{"code":0,"data":null}""")
        })
        val result = api.register(RegisterRequest(UserRegister("tom", "pw", "t@e.c"), "cap"))
        assertEquals("/user/register", path)
        assertTrue(body.contains("tom"))
        assertTrue(body.contains("cap"))
        assertTrue(result is NetworkResult.Empty)
    }

    @Test
    fun `verifyEmail posts email`() = runTest {
        var path = ""
        val api = AuthApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            jsonBody("""{"code":0,"data":null}""")
        })
        val result = api.verifyEmail(EmailVerifyRequest("t@e.c"))
        assertEquals("/token/verify_email", path)
        assertTrue(result is NetworkResult.Empty)
    }

    @Test
    fun `getCaptcha carries typed byte result`() = runTest {
        // 验证 getCaptcha 的失败路径
        val api = AuthApiImpl(buildTestApiClient { throw RuntimeException("io") })
        val result = api.getCaptcha()
        assertTrue(result is NetworkResult.Failure)
    }
}
