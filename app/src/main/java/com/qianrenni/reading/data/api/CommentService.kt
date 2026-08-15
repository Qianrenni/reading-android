package com.qianrenni.reading.data.api

import com.qianrenni.reading.data.model.BookChapterComment
import com.qianrenni.reading.data.model.BookComment
import com.qianrenni.reading.data.model.BookReviewRequest
import com.qianrenni.reading.data.model.CommentPageResult
import com.qianrenni.reading.data.model.LineCommentRequest
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody

/**
 * 评论相关 API：书评 + 章节行评论
 */
object CommentService {

    /** 分页获取书评列表 */
    suspend fun getBookReviews(
        bookId: Int,
        page: Int = 1,
        size: Int = 20
    ): NetworkResult<CommentPageResult> {
        return NetworkClient.get("comment/book/$bookId") {
            parameter("page", page)
            parameter("size", size)
        }
    }

    /** 获取自己的书评（可能为 null） */
    suspend fun getMyBookReview(bookId: Int): NetworkResult<BookComment?> {
        return NetworkClient.get("comment/book/$bookId/mine")
    }

    /** 发布/编辑书评 */
    suspend fun createBookReview(bookId: Int, content: String): NetworkResult<Unit> {
        return NetworkClient.post("comment/book/$bookId") {
            setBody(BookReviewRequest(content))
        }
    }

    /** 删除自己的书评 */
    suspend fun deleteBookReview(bookId: Int): NetworkResult<Unit> {
        return NetworkClient.delete("comment/book/$bookId")
    }

    /** 获取某章所有行评论（Map<行号, 评论列表>） */
    suspend fun getChapterComments(
        bookId: Int,
        chapterId: Int
    ): NetworkResult<Map<Int, List<BookChapterComment>>> {
        return NetworkClient.get("comment/chapter/$bookId/$chapterId")
    }

    /** 发表/更新某行评论 */
    suspend fun createLineComment(
        bookId: Int,
        chapterId: Int,
        line: Int,
        content: String
    ): NetworkResult<Unit> {
        return NetworkClient.post("comment/chapter/$bookId/$chapterId") {
            setBody(LineCommentRequest(line, content))
        }
    }

    /** 删除自己的行评论 */
    suspend fun deleteLineComment(
        bookId: Int,
        chapterId: Int,
        commentId: Int
    ): NetworkResult<Unit> {
        return NetworkClient.delete("comment/chapter/$bookId/$chapterId") {
            parameter("commentId", commentId)
        }
    }
}
