package com.qianrenni.reading.data.api

import com.qianrenni.reading.data.model.BookReadingProgress
import com.qianrenni.reading.data.model.UpdateProgressRequest
import io.ktor.client.request.setBody

object ReadingProgressService {
    suspend fun getReadingProgress(): NetworkResult<List<BookReadingProgress>> {
        return NetworkClient.get("user_reading_progress/get")
    }

    suspend fun updateReadingProgress(
        request: UpdateProgressRequest
    ): NetworkResult<Unit> {
        return NetworkClient.patch("user_reading_progress/add") {
            setBody(request)
        }
    }

    suspend fun deleteReadingProgress(
        bookId: Int
    ): NetworkResult<Unit> {
        return NetworkClient.delete("user_reading_progress/delete/$bookId")
    }
}