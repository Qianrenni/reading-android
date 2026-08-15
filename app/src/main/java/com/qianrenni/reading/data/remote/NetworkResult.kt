package com.qianrenni.reading.data.remote

/**
 * 统一的网络结果类型（Success / Empty / Failure）。
 */
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
