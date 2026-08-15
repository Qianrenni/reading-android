package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.BookChapterComment
import com.qianrenni.reading.data.model.BookComment
import com.qianrenni.reading.data.model.BookReviewRequest
import com.qianrenni.reading.data.model.CommentPageResult
import com.qianrenni.reading.data.model.LineCommentRequest
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody

/**
 * 评论相关 API：书评 + 章节行评论。
 */
interface CommentApi {
    /** 分页获取书评列表 */
    suspend fun getBookReviews(bookId: Int, page: Int, size: Int): NetworkResult<CommentPageResult>
    /** 获取自己的书评（可能为 null） */
    suspend fun getMyBookReview(bookId: Int): NetworkResult<BookComment?>
    /** 发布/编辑书评 */
    suspend fun createBookReview(bookId: Int, content: String): NetworkResult<Unit>
    /** 删除自己的书评 */
    suspend fun deleteBookReview(bookId: Int): NetworkResult<Unit>
    /** 获取某章所有行评论（Map<行号, 评论列表>） */
    suspend fun getChapterComments(bookId: Int, chapterId: Int): NetworkResult<Map<Int, List<BookChapterComment>>>
    /** 发表/更新某行评论 */
    suspend fun createLineComment(bookId: Int, chapterId: Int, line: Int, content: String): NetworkResult<Unit>
    /** 删除自己的行评论 */
    suspend fun deleteLineComment(bookId: Int, chapterId: Int, commentId: Int): NetworkResult<Unit>
}

class CommentApiImpl(private val apiClient: ApiClient) : CommentApi {

    override suspend fun getBookReviews(bookId: Int, page: Int, size: Int): NetworkResult<CommentPageResult> {
        return apiClient.get("comment/book/$bookId") {
            parameter("page", page)
            parameter("size", size)
        }
    }

    override suspend fun getMyBookReview(bookId: Int): NetworkResult<BookComment?> {
        return apiClient.get("comment/book/$bookId/mine")
    }

    override suspend fun createBookReview(bookId: Int, content: String): NetworkResult<Unit> {
        return apiClient.post("comment/book/$bookId") {
            setBody(BookReviewRequest(content))
        }
    }

    override suspend fun deleteBookReview(bookId: Int): NetworkResult<Unit> {
        return apiClient.delete("comment/book/$bookId")
    }

    override suspend fun getChapterComments(bookId: Int, chapterId: Int): NetworkResult<Map<Int, List<BookChapterComment>>> {
        return apiClient.get("comment/chapter/$bookId/$chapterId")
    }

    override suspend fun createLineComment(
        bookId: Int,
        chapterId: Int,
        line: Int,
        content: String
    ): NetworkResult<Unit> {
        return apiClient.post("comment/chapter/$bookId/$chapterId") {
            setBody(LineCommentRequest(line, content))
        }
    }

    override suspend fun deleteLineComment(
        bookId: Int,
        chapterId: Int,
        commentId: Int
    ): NetworkResult<Unit> {
        return apiClient.delete("comment/chapter/$bookId/$chapterId") {
            parameter("commentId", commentId)
        }
    }
}
