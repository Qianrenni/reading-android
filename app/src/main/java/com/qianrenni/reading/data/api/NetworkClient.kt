package com.qianrenni.reading.data.api

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


object NetworkClient {
    private const val BASE_URL = "http://49.235.107.221:8000/" // 请替换为实际的 API 基础 URL
    const val TAG = "NetworkClient"
    private var token = ""
        set(value) = run {
            field = value
        }

    fun getToken() = token
    private var tokenType = ""
        set(value) = run {
            field = value
        }

    fun getTokenType() = tokenType
    fun setToken(token: String, tokenType: String) {
        this.token = token
        this.tokenType = tokenType
    }

    val client: HttpClient by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }

            engine {
                connectTimeout = 30_000
                socketTimeout = 30_000
            }
        }
    }

    fun getBaseUrl(): String = BASE_URL

    /**
     * GET请求 - 模仿Web项目的get方法
     */
    suspend inline fun <reified T> get(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): NetworkResult<T> {
        try {
            val response: HttpResponse = client.get(getBaseUrl() + urlString) {
                header("Authorization", "${getTokenType()} ${getToken()}")
                contentType(ContentType.Application.Json)
                block()
            }
            return ResponseHandler.handleResponse<T>(response)
        } catch (e: Exception) {
            Log.e(TAG, "get: ${e.message}")
            return NetworkResult.Failure(message = "网络错误:服务器连接异常", exception = e)
        }
    }

    /**
     * POST请求 - 模仿Web项目的post方法
     */
    suspend inline fun <reified T> post(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): NetworkResult<T> {
        try {
            val response: HttpResponse = client.post(getBaseUrl() + urlString) {
                header("Authorization", "${getTokenType()} ${getToken()}")
                contentType(ContentType.Application.Json)
                block()
            }
            return ResponseHandler.handleResponse<T>(response)
        } catch (e: Exception) {
            Log.e(TAG, "get: ${e.message}")
            return NetworkResult.Failure(message = "网络错误:服务器连接异常", exception = e)
        }
    }

    /**
     * PUT请求 - 模仿Web项目的put方法
     */
    suspend inline fun <reified T> put(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): NetworkResult<T> {
        try {
            val response: HttpResponse = client.put(getBaseUrl() + urlString) {
                header("Authorization", "${getTokenType()} ${getToken()}")
                contentType(ContentType.Application.Json)
                block()
            }
            return ResponseHandler.handleResponse<T>(response)
        } catch (e: Exception) {
            Log.e(TAG, "get: ${e.message}")
            return NetworkResult.Failure(message = "网络错误:服务器连接异常", exception = e)
        }
    }

    /**
     * DELETE请求 - 模仿Web项目的del方法
     */
    suspend inline fun <reified T> delete(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): NetworkResult<T> {
        try {
            val response: HttpResponse = client.delete(getBaseUrl() + urlString) {
                header("Authorization", "${getTokenType()} ${getToken()}")
                contentType(ContentType.Application.Json)
                block()
            }
            return ResponseHandler.handleResponse<T>(response)
        } catch (e: Exception) {
            Log.e(TAG, "get: ${e.message}")
            return NetworkResult.Failure(message = "网络错误:服务器连接异常", exception = e)
        }
    }

    /**
     * PATCH请求 - 模仿Web项目的patch方法
     */
    suspend inline fun <reified T> patch(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): NetworkResult<T> {
        try {
            val response: HttpResponse = client.patch(getBaseUrl() + urlString) {
                header("Authorization", "${getTokenType()} ${getToken()}")
                contentType(ContentType.Application.Json)
                block()
            }
            return ResponseHandler.handleResponse<T>(response)
        } catch (e: Exception) {
            Log.e(TAG, "get: ${e.message}")
            return NetworkResult.Failure(message = "网络错误:服务器连接异常", exception = e)
        }
    }
}
