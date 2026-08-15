package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.BookComment
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentApiTest {

    private val reviewJson = """{"code":0,"data":{"items":[{"id":1,"bookId":5,"userId":9,"userName":"u","content":"好"},{"id":2,"bookId":5,"userId":9,"userName":"u","content":"棒"}],"total":2}}"""

    @Test
    fun `getBookReviews sends page and size params`() = runTest {
        var path = ""
        val api = CommentApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            assertEquals("2", req.url.parameters["page"])
            assertEquals("20", req.url.parameters["size"])
            jsonBody(reviewJson)
        })
        val result = api.getBookReviews(5, page = 2, size = 20)
        assertEquals("/comment/book/5", path)
        assertTrue(result is NetworkResult.Success)
        assertEquals(2, (result as NetworkResult.Success).data.items.size)
    }

    @Test
    fun `getMyBookReview returns null data as empty`() = runTest {
        val api = CommentApiImpl(buildTestApiClient { jsonBody("""{"code":0,"data":null}""") })
        val result = api.getMyBookReview(5)
        assertTrue(result is NetworkResult.Empty)
    }

    @Test
    fun `getMyBookReview parses review`() = runTest {
        val api = CommentApiImpl(
            buildTestApiClient {
                jsonBody("""{"code":0,"data":{"id":1,"bookId":5,"userId":9,"userName":"u","content":"好"}}""")
            }
        )
        val result = api.getMyBookReview(5)
        assertTrue(result is NetworkResult.Success)
        assertEquals("好", (result as NetworkResult.Success).data?.content)
    }

    @Test
    fun `createBookReview posts content`() = runTest {
        var path = ""
        var method = ""
        var body = ""
        val api = CommentApiImpl(buildTestApiClient { req ->
            path = req.url.encodedPath
            method = req.method.value
            body = req.bodyText()
            jsonBody("""{"code":0,"data":null}""")
        })
        val result = api.createBookReview(5, "不错")
        assertEquals("/comment/book/5", path)
        assertEquals("POST", method)
        assertTrue(body.contains("不错"))
        assertTrue(result is NetworkResult.Empty)
    }

    @Test
    fun `getChapterComments parses map`() = runTest {
        val api = CommentApiImpl(
            buildTestApiClient {
                jsonBody("""{"code":0,"data":{"10":[{"id":1,"chapterId":3,"userId":9,"userName":"u","content":"x","line":10}]}}""")
            }
        )
        val result = api.getChapterComments(5, 3)
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data[10]?.size)
    }

    @Test
    fun `deleteLineComment sends commentId param`() = runTest {
        val api = CommentApiImpl(buildTestApiClient { req ->
            assertEquals("42", req.url.parameters["commentId"])
            jsonBody("""{"code":0,"data":null}""")
        })
        val result = api.deleteLineComment(5, 3, commentId = 42)
        assertTrue(result is NetworkResult.Empty)
    }
}
