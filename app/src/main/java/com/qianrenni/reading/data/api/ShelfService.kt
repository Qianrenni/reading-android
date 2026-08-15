package com.qianrenni.reading.data.api

import com.qianrenni.reading.data.model.AddShelfRequest
import com.qianrenni.reading.data.model.ShelfItem
import io.ktor.client.request.setBody

object ShelfService {
    suspend fun getShelf(): NetworkResult<List<ShelfItem>> {
        return NetworkClient.get("shelf/get")
    }

    suspend fun addToShelf(
        request: AddShelfRequest
    ): NetworkResult<Unit> {
        return NetworkClient.post("shelf/add") {
            setBody(request)
        }
    }

    suspend fun removeFromShelf(
        bookId: Int
    ): NetworkResult<Unit> {
        return NetworkClient.delete("shelf/delete/$bookId")
    }
}