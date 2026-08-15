package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.AddShelfRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfApiTest {

    @Test
    fun `getShelf parses items`() = runTest {
        var path = ""
        val api = ShelfApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            jsonBody("""{"code":0,"data":[{"bookId":1,"createdAt":"2026-01-01"}]}""")
        })
        val result = api.getShelf()
        assertEquals("/shelf/get", path)
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data.size)
    }

    @Test
    fun `addToShelf posts request`() = runTest {
        var path = ""
        var body = ""
        val api = ShelfApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            body = req.bodyText()
            jsonBody("""{"code":0,"data":null}""")
        })
        val result = api.addToShelf(AddShelfRequest(3))
        assertEquals("/shelf/add", path)
        assertTrue(body.contains("bookId"))
        assertTrue(result is NetworkResult.Empty)
    }

    @Test
    fun `removeFromShelf uses DELETE`() = runTest {
        var path = ""
        var method = ""
        val api = ShelfApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            method = req.method.value
            jsonBody("""{"code":0,"data":null}""")
        })
        val result = api.removeFromShelf(3)
        assertEquals("/shelf/delete/3", path)
        assertEquals("DELETE", method)
        assertTrue(result is NetworkResult.Empty)
    }
}
