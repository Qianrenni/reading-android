package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.AddShelfRequest
import com.qianrenni.reading.data.model.ShelfItem
import io.ktor.client.request.setBody

/**
 * 书架相关 API。
 */
interface ShelfApi {
    suspend fun getShelf(): NetworkResult<List<ShelfItem>>
    suspend fun addToShelf(request: AddShelfRequest): NetworkResult<Unit>
    suspend fun removeFromShelf(bookId: Int): NetworkResult<Unit>
}

class ShelfApiImpl(private val apiClient: ApiClient) : ShelfApi {

    override suspend fun getShelf(): NetworkResult<List<ShelfItem>> {
        return apiClient.get("shelf/get")
    }

    override suspend fun addToShelf(request: AddShelfRequest): NetworkResult<Unit> {
        return apiClient.post("shelf/add") {
            setBody(request)
        }
    }

    override suspend fun removeFromShelf(bookId: Int): NetworkResult<Unit> {
        return apiClient.delete("shelf/delete/$bookId")
    }
}
