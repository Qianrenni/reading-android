package com.qianrenni.reading.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BookReadingProgress(
    val bookId: Int,
    val lastChapterId: Int,
    val lastPosition: Int = 0,
    val lastReadAt: String = ""
)

@Serializable
data class UpdateProgressRequest(
    val bookId: Int,
    val lastChapterId: Int,
    val lastPosition: Int = 0
)

@Serializable
data class ShelfItem(
    val bookId: Int,
    val createdAt: String = "",
    // 合并阅读进度信息
    val lastChapterId: Int? = null,
    val lastPosition: Int? = null,
    val lastReadAt: String? = null
)

@Serializable
data class AddShelfRequest(
    val bookId: Int
)

@Serializable
data class ReadEvent(
    val bookId: Int,
    val chapterId: Int,
    val eventType: String // "enter", "exit", "heartbeat"
)