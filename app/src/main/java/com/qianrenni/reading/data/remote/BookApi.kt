package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.Book
import com.qianrenni.reading.data.model.Catalog
import io.ktor.client.request.parameter

/**
 * 书籍相关 API。
 */
interface BookApi {
    suspend fun getCategories(): NetworkResult<List<String>>
    suspend fun getBooksByCategory(
        category: String,
        offset: Int = 0,
        limit: Int = 25,
        sort: String = "id:-1"
    ): NetworkResult<Array<Book>>
    suspend fun searchBooks(query: String): NetworkResult<Array<Book>>
    suspend fun getBookById(bookId: Int): NetworkResult<Book>
    suspend fun getCatalog(bookId: Int): NetworkResult<Array<Catalog>>
    suspend fun getChapter(chapterId: Int, bookId: Int): NetworkResult<String>
    suspend fun getRecommendations(query: String = "tags"): NetworkResult<Array<Book>>
    suspend fun getBooksByIds(bookIds: List<Int>): NetworkResult<Array<Book>>
}

class BookApiImpl(private val apiClient: ApiClient) : BookApi {

    override suspend fun getCategories(): NetworkResult<List<String>> {
        return apiClient.get("book/category")
    }

    override suspend fun getBooksByCategory(
        category: String,
        offset: Int,
        limit: Int,
        sort: String
    ): NetworkResult<Array<Book>> {
        return apiClient.get("book/select") {
            parameter("category", category)
            parameter("offset", offset)
            parameter("limit", limit)
            parameter("sort", sort)
        }
    }

    override suspend fun searchBooks(query: String): NetworkResult<Array<Book>> {
        return apiClient.get("book/search") {
            parameter("q", query)
        }
    }

    override suspend fun getBookById(bookId: Int): NetworkResult<Book> {
        return apiClient.get("book/$bookId")
    }

    override suspend fun getCatalog(bookId: Int): NetworkResult<Array<Catalog>> {
        return apiClient.get("book/toc/$bookId")
    }

    override suspend fun getChapter(chapterId: Int, bookId: Int): NetworkResult<String> {
        return apiClient.get("book/chapter/$chapterId") {
            parameter("bookId", bookId)
        }
    }

    override suspend fun getRecommendations(query: String): NetworkResult<Array<Book>> {
        return apiClient.get("book/recommend") {
            parameter("query", query)
        }
    }

    override suspend fun getBooksByIds(bookIds: List<Int>): NetworkResult<Array<Book>> {
        return apiClient.get("book/list") {
            bookIds.forEach { parameter("bookIds", it) }
        }
    }
}
