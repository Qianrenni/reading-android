package com.qianrenni.reading.data.remote

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * 网络客户端封装：统一设置 JSON Content-Type 并通过 [ResponseHandler] 规范化结果。
 *
 * - Authorization 头由 Ktor Auth(Bearer) 插件自动注入（见 [HttpClientFactory.createAuthClient]）；
 * - [baseUrlProvider] 让 base url 在运行期可动态修改（应用内调整服务器地址）。
 *
 * @param onUnauthorized 401 且刷新失败时回调（用于清理会话）
 */
class ApiClient(
    val client: HttpClient,
    @PublishedApi internal val baseUrlProvider: () -> String,
    @PublishedApi internal val onUnauthorized: () -> Unit = {}
) {

    /** 当前 base url（供需要直接访问原始 client 的调用方使用）。 */
    val baseUrl: String
        get() = baseUrlProvider()

    /**
     * GET 请求。
     */
    suspend inline fun <reified T> get(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): NetworkResult<T> {
        try {
            val response: HttpResponse = client.get(baseUrlProvider() + urlString) {
                contentType(ContentType.Application.Json)
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
            val response: HttpResponse = client.post(baseUrlProvider() + urlString) {
                contentType(ContentType.Application.Json)
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
            val response: HttpResponse = client.put(baseUrlProvider() + urlString) {
                contentType(ContentType.Application.Json)
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
            val response: HttpResponse = client.delete(baseUrlProvider() + urlString) {
                contentType(ContentType.Application.Json)
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
            val response: HttpResponse = client.patch(baseUrlProvider() + urlString) {
                contentType(ContentType.Application.Json)
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
