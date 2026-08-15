package com.qianrenni.reading.data.api

import com.qianrenni.reading.data.model.ReadEvent
import io.ktor.client.request.setBody

object ReportService {
    suspend fun reportChapterRead(
        event: ReadEvent
    ): NetworkResult<Unit> {
        return NetworkClient.post("statistic/book-chapter") {
            setBody(event)
        }
    }
}