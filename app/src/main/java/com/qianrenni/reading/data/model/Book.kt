package com.qianrenni.reading.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val id: Int,
    val name: String,
    val author: String,
    val cover: String,
    val description: String = "",
    val category: String = "",
    val tags: String = "",
    val totalChapter: Int = 0,
    val createdAt: String = "",
    val isEnded: Boolean = false,
    val wordsCount: Int = 0
)

@Serializable
data class Catalog(
    val id: Int,
    val title: String,
    val wordsCount: Int = 0,
    val order: Double = 0.0,
    val createdAt: String = ""
)