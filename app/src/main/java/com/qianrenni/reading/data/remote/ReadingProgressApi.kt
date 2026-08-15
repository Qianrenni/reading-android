package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.BookReadingProgress
import com.qianrenni.reading.data.model.UpdateProgressRequest
import io.ktor.client.request.setBody

/**
 * 阅读进度相关 API。
 */
interface ReadingProgressApi {
    suspend fun getReadingProgress(): NetworkResult<List<BookReadingProgress>>
    suspend fun updateReadingProgress(request: UpdateProgressRequest): NetworkResult<Unit>
    suspend fun deleteReadingProgress(bookId: Int): NetworkResult<Unit>
}

class ReadingProgressApiImpl(private val apiClient: ApiClient) : ReadingProgressApi {

    override suspend fun getReadingProgress(): NetworkResult<List<BookReadingProgress>> {
        return apiClient.get("user_reading_progress/get")
    }

    override suspend fun updateReadingProgress(request: UpdateProgressRequest): NetworkResult<Unit> {
        return apiClient.patch("user_reading_progress/add") {
            setBody(request)
        }
    }

    override suspend fun deleteReadingProgress(bookId: Int): NetworkResult<Unit> {
        return apiClient.delete("user_reading_progress/delete/$bookId")
    }
}
