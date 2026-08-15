package com.qianrenni.reading.data.api

import com.qianrenni.reading.data.model.Book
import com.qianrenni.reading.data.model.Catalog
import io.ktor.client.request.parameter

object BookService {
    suspend fun getCategories(): NetworkResult<List<String>> {
        return NetworkClient.get("book/category")
    }

    suspend fun getBooksByCategory(
        category: String,
        offset: Int = 0,
        limit: Int = 25,
        sort: String = "id:-1"
    ): NetworkResult<Array<Book>> {
        return NetworkClient.get("book/select") {
            parameter("category", category)
            parameter("offset", offset)
            parameter("limit", limit)
            parameter("sort", sort)
        }
    }

    suspend fun searchBooks(
        query: String
    ): NetworkResult<Array<Book>> {
        return NetworkClient.get("book/search") {
            parameter("q", query)
        }
    }

    suspend fun getBookById(
        bookId: Int
    ): NetworkResult<Book> {
        return NetworkClient.get("book/$bookId")
    }

    suspend fun getCatalog(
        bookId: Int
    ): NetworkResult<Array<Catalog>> {
        return NetworkClient.get("book/toc/$bookId")
    }

    suspend fun getChapter(
        chapterId: Int,
        bookId: Int
    ): NetworkResult<String> {
        return NetworkClient.get("book/chapter/$chapterId") {
            parameter("bookId", bookId)
        }
    }

    suspend fun getRecommendations(
        query: String = "tags"
    ): NetworkResult<Array<Book>> {
        return NetworkClient.get("book/recommend") {
            parameter("query", query)
        }
    }

    suspend fun getBooksByIds(
        bookIds: List<Int>
    ): NetworkResult<Array<Book>> {
        return NetworkClient.get("book/list") {
            bookIds.forEach { parameter("bookIds", it) }
        }
    }
}
