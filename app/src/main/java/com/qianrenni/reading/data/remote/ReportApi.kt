package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.ReadEvent
import io.ktor.client.request.setBody

/**
 * 数据上报相关 API。
 */
interface ReportApi {
    suspend fun reportChapterRead(event: ReadEvent): NetworkResult<Unit>
}

class ReportApiImpl(private val apiClient: ApiClient) : ReportApi {

    override suspend fun reportChapterRead(event: ReadEvent): NetworkResult<Unit> {
        return apiClient.post("statistic/book-chapter") {
            setBody(event)
        }
    }
}
