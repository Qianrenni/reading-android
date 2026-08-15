package com.qianrenni.reading.data.api

import android.util.Log
import com.qianrenni.reading.data.model.ApiResponse
import com.qianrenni.reading.data.store.AuthStore
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.contentType

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Empty(val code: Int = 204) : NetworkResult<Nothing>()
    data class Failure(
        val message: String,
        val code: Int? = null,
        val exception: Throwable? = null
    ) : NetworkResult<Nothing>()

    // 函数式风格处理
    suspend fun fold(
        onSuccess: suspend (T) -> Unit = {},
        onFailure: suspend (String, Int?, Throwable?) -> Unit = { _, _, _ -> },
        onEmpty: suspend () -> Unit = {}
    ): NetworkResult<T> {
        when (this) {
            is Success -> onSuccess(data)
            is Failure -> onFailure(message, code, exception)
            is Empty -> onEmpty()
        }
        return this
    }

    suspend fun <R> onSuccess(block: suspend (T) -> R): R? {
        return when (this) {
            is Success -> block(data)
            else -> null
        }
    }

    suspend fun <R> onFailure(block: suspend (String, Int?, Throwable?) -> R): R? {
        return when (this) {
            is Failure -> block(message, code, exception)
            else -> null
        }
    }

    suspend fun <R> onEmpty(block: suspend () -> R): R? {
        return when (this) {
            is Empty -> block()
            else -> null
        }
    }
}

/**
 * 统一的网络响应处理器，responseHandler设计
 */
object ResponseHandler {

    const val SUCCESS_CODE = 0
    const val FAILURE_CODE = 1

    /**
     * 处理HTTP响应，返回标准化的结果
     */
    suspend inline fun <reified T> handleResponse(response: HttpResponse): NetworkResult<T> {
        val statusCode = response.status.value
        val contentType = response.contentType()

        Log.d(
            "handleResponse", """
            Content-Type: ${contentType.toString()}
            Url: ${response.request.url}
            Status Code: $statusCode
            Body: ${response.bodyAsText()}
        """.trimIndent()
        )

        // 特殊处理 204 No Content
        if (listOf(204, 201).contains(statusCode)) {
            return NetworkResult.Empty(statusCode)
        }
        if (statusCode == 401) {
            AuthStore.setUser(null)
            return NetworkResult.Failure("身份信息失效", statusCode)
        }
        return if (contentType?.match(ContentType.Application.Json) == true) {
            try {
                val apiResponse = response.body<ApiResponse<T>>()

                if (apiResponse.code == SUCCESS_CODE) {
                    // 成功保证 data 非空（如果后端可能返回 null，需额外校验）
                    apiResponse.data?.let {
                        NetworkResult.Success(it)
                    } ?: NetworkResult.Empty(code = statusCode)
                } else {
                    NetworkResult.Failure(
                        message = apiResponse.message ?: "操作失败",
                        code = apiResponse.code
                    )
                }
            } catch (e: Exception) {
                Log.e("NETWORK ERROR", e.message.orEmpty())
                NetworkResult.Failure(
                    message = e.message ?: "解析失败",
                    exception = e
                )
            }
        } else {
            val success = isHttpSuccess(statusCode)
            if (success) {
                NetworkResult.Failure("非JSON响应但状态成功", statusCode)
            } else {
                NetworkResult.Failure("请求失败", statusCode)
            }
        }
    }

    /**
     * 判断HTTP状态码是否表示成功
     */
    fun isHttpSuccess(statusCode: Int): Boolean {
        return statusCode in 200..299
    }
}