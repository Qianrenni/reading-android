package com.qianrenni.reading.data.remote

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookApiTest {

    private val bookJson =
        """{"code":0,"data":{"id":1,"name":"书","author":"作者","cover":"c"}}"""
    private val rawBookJson =
        """{"id":1,"name":"书","author":"作者","cover":"c"}"""

    @Test
    fun `getCategories returns list`() = runTest {
        var path = ""
        val api = BookApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            jsonBody("""{"code":0,"data":["玄幻","都市"]}""")
        })
        val result = api.getCategories()
        assertEquals("/book/category", path)
        assertTrue(result is NetworkResult.Success)
        assertEquals(listOf("玄幻", "都市"), (result as NetworkResult.Success).data)
    }

    @Test
    fun `getBooksByCategory sends query params`() = runTest {
        var path = ""
        val api = BookApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            assertEquals("玄幻", req.url.parameters["category"])
            assertEquals("10", req.url.parameters["offset"])
            assertEquals("25", req.url.parameters["limit"])
            assertEquals("id:-1", req.url.parameters["sort"])
            jsonBody("""{"code":0,"data":[$rawBookJson]}""")
        })
        val result = api.getBooksByCategory("玄幻", 10, 25, "id:-1")
        assertEquals("/book/select", path)
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data.size)
    }

    @Test
    fun `getBookById parses single book`() = runTest {
        var path = ""
        val api = BookApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            jsonBody(bookJson)
        })
        val result = api.getBookById(1)
        assertEquals("/book/1", path)
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data.id)
    }

    @Test
    fun `getBooksByIds sends repeated bookIds params`() = runTest {
        var path = ""
        var paramCount = 0
        val api = BookApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            paramCount = req.url.parameters.getAll("bookIds")?.size ?: 0
            jsonBody("""{"code":0,"data":[$rawBookJson]}""")
        })
        val result = api.getBooksByIds(listOf(1, 2, 3))
        assertEquals("/book/list", path)
        assertEquals(3, paramCount)
        assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun `getChapter returns content string`() = runTest {
        var path = ""
        val api = BookApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            jsonBody("""{"code":0,"data":"第一章内容"}""")
        })
        val result = api.getChapter(9, 1)
        assertEquals("/book/chapter/9", path)
        assertTrue(result is NetworkResult.Success)
        assertEquals("第一章内容", (result as NetworkResult.Success).data)
    }
}
