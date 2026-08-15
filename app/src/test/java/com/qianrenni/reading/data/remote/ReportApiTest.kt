package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.ReadEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportApiTest {

    @Test
    fun `reportChapterRead posts event with enter type`() = runTest {
        var path = ""
        var method = ""
        var body = ""
        val api = ReportApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            method = req.method.value
            body = req.bodyText()
            jsonBody("""{"code":0,"data":null}""")
        })
        val result = api.reportChapterRead(ReadEvent(1, 2, "enter"))
        assertEquals("/statistic/book-chapter", path)
        assertEquals("POST", method)
        assertTrue(body.contains("enter"))
        assertTrue(result is NetworkResult.Empty)
    }
}
