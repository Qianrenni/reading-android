package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.UpdateProgressRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingProgressApiTest {

    @Test
    fun `getReadingProgress parses list`() = runTest {
        var path = ""
        val api = ReadingProgressApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            jsonBody("""{"code":0,"data":[{"bookId":1,"lastChapterId":2,"lastPosition":3,"lastReadAt":"2026-01-01"}]}""")
        })
        val result = api.getReadingProgress()
        assertEquals("/user_reading_progress/get", path)
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data.size)
        assertEquals(2, result.data[0].lastChapterId)
    }

    @Test
    fun `updateReadingProgress uses PATCH with body`() = runTest {
        var path = ""
        var method = ""
        var body = ""
        val api = ReadingProgressApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            method = req.method.value
            body = req.bodyText()
            jsonBody("""{"code":0,"data":null}""")
        })
        val result = api.updateReadingProgress(UpdateProgressRequest(1, 2))
        assertEquals("/user_reading_progress/add", path)
        assertEquals("PATCH", method)
        assertTrue(body.contains("lastChapterId"))
        assertTrue(result is NetworkResult.Empty)
    }

    @Test
    fun `deleteReadingProgress uses DELETE`() = runTest {
        var path = ""
        var method = ""
        val api = ReadingProgressApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            method = req.method.value
            jsonBody("""{"code":0,"data":null}""")
        })
        val result = api.deleteReadingProgress(7)
        assertEquals("/user_reading_progress/delete/7", path)
        assertEquals("DELETE", method)
        assertTrue(result is NetworkResult.Empty)
    }
}
