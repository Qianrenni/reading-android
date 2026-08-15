package com.qianrenni.reading.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: Int = 1,
    val message: String? = null,
    val data: T? = null
) {
    val isSuccess: Boolean
        get() = code == 0
}