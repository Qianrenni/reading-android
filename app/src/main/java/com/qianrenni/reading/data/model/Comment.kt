package com.qianrenni.reading.data.model

import kotlinx.serialization.Serializable

/**
 * 书评信息
 */
@Serializable
data class BookComment(
    val id: Int,
    val bookId: Int,
    val userId: Int,
    val userName: String,
    val userAvatar: String = "",
    val content: String,
    val status: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val parentId: Int? = null
)

/**
 * 章节行评论
 */
@Serializable
data class BookChapterComment(
    val id: Int,
    val chapterId: Int,
    val userId: Int,
    val userName: String,
    val userAvatar: String = "",
    val content: String,
    val status: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val parentId: Int? = null,
    val line: Int
)

/**
 * 书评分页结果
 */
@Serializable
data class CommentPageResult(
    val items: List<BookComment> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val size: Int = 20
)

@Serializable
data class BookReviewRequest(val content: String)

@Serializable
data class LineCommentRequest(val line: Int, val content: String)
