package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.ForgotPasswordRequest
import com.qianrenni.reading.data.model.UpdatePasswordRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserApiTest {

    @Test
    fun `updatePassword uses PATCH`() = runTest {
        var path = ""
        var method = ""
        val api = UserApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            method = req.method.value
            jsonBody("""{"code":0,"data":null}""")
        })
        val result = api.updatePassword(UpdatePasswordRequest("t@e.c", "old", "new"))
        assertEquals("/user/update-password", path)
        assertEquals("PATCH", method)
        assertTrue(result is NetworkResult.Empty)
    }

    @Test
    fun `sendForgotPasswordCode sends user_account param`() = runTest {
        var path = ""
        val api = UserApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            assertEquals("t@e.c", req.url.parameters["user_account"])
            jsonBody("""{"code":0,"data":null}""")
        })
        val result = api.sendForgotPasswordCode("t@e.c")
        assertEquals("/user/forgot-password", path)
        assertTrue(result is NetworkResult.Empty)
    }

    @Test
    fun `resetPassword uses PATCH with body`() = runTest {
        var method = ""
        var body = ""
        val api = UserApiImpl(buildTestApiClient { req ->
            method = req.method.value
            body = req.bodyText()
            jsonBody("""{"code":0,"data":null}""")
        })
        val result = api.resetPassword(ForgotPasswordRequest("t@e.c", "code", "new"))
        assertEquals("PATCH", method)
        assertTrue(body.contains("code"))
        assertTrue(result is NetworkResult.Empty)
    }
}
