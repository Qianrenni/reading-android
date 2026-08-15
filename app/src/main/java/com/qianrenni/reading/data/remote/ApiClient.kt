package com.qianrenni.reading.data.remote

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

const val DEFAULT_BASE_URL = "http://49.235.107.221:8000/"

/**
 * 网络客户端封装：统一注入 Authorization 头、JSON Content-Type，
 * 并通过 [ResponseHandler] 规范化结果。
 *
 * @param tokenProvider 返回完整 Authorization 头值（如 "Bearer xxx"），无令牌返回 null
 * @param onUnauthorized 401 时回调（用于清理会话）
 */
class ApiClient(
    val client: HttpClient,
    val baseUrl: String = DEFAULT_BASE_URL,
    @PublishedApi internal val tokenProvider: () -> String? = { null },
    @PublishedApi internal val onUnauthorized: () -> Unit = {}
) {

    @PublishedApi
    internal fun HttpRequestBuilder.applyAuth() {
        tokenProvider()?.let { header("Authorization", it) }
        contentType(ContentType.Application.Json)
    }

    /**
     * GET 请求。
     */
    suspend inline fun <reified T> get(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): NetworkResult<T> {
        try {
            val response: HttpResponse = client.get(baseUrl + urlString) {
                applyAuth()
                block()
            }
            return ResponseHandler.handleResponse<T>(response, onUnauthorized)
        } catch (e: Exception) {
            Log.e(TAG, "get: ${e.message}")
            return NetworkResult.Failure(message = "网络错误:服务器连接异常", exception = e)
        }
    }

    /**
     * POST 请求。
     */
    suspend inline fun <reified T> post(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): NetworkResult<T> {
        try {
            val response: HttpResponse = client.post(baseUrl + urlString) {
                applyAuth()
                block()
            }
            return ResponseHandler.handleResponse<T>(response, onUnauthorized)
        } catch (e: Exception) {
            Log.e(TAG, "post: ${e.message}")
            return NetworkResult.Failure(message = "网络错误:服务器连接异常", exception = e)
        }
    }

    /**
     * PUT 请求。
     */
    suspend inline fun <reified T> put(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): NetworkResult<T> {
        try {
            val response: HttpResponse = client.put(baseUrl + urlString) {
                applyAuth()
                block()
            }
            return ResponseHandler.handleResponse<T>(response, onUnauthorized)
        } catch (e: Exception) {
            Log.e(TAG, "put: ${e.message}")
            return NetworkResult.Failure(message = "网络错误:服务器连接异常", exception = e)
        }
    }

    /**
     * DELETE 请求。
     */
    suspend inline fun <reified T> delete(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): NetworkResult<T> {
        try {
            val response: HttpResponse = client.delete(baseUrl + urlString) {
                applyAuth()
                block()
            }
            return ResponseHandler.handleResponse<T>(response, onUnauthorized)
        } catch (e: Exception) {
            Log.e(TAG, "delete: ${e.message}")
            return NetworkResult.Failure(message = "网络错误:服务器连接异常", exception = e)
        }
    }

    /**
     * PATCH 请求。
     */
    suspend inline fun <reified T> patch(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): NetworkResult<T> {
        try {
            val response: HttpResponse = client.patch(baseUrl + urlString) {
                applyAuth()
                block()
            }
            return ResponseHandler.handleResponse<T>(response, onUnauthorized)
        } catch (e: Exception) {
            Log.e(TAG, "patch: ${e.message}")
            return NetworkResult.Failure(message = "网络错误:服务器连接异常", exception = e)
        }
    }

    companion object {
        const val TAG = "ApiClient"
    }
}
