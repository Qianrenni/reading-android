package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * 令牌刷新器：使用 refresh token 调用 /token/refresh 获取新令牌。
 *
 * 接口抽象便于 AuthRepository 单元测试时替换为 Fake。
 */
interface TokenRefresher {
    suspend fun refresh(tokenType: String, refreshToken: String): LoginResponse?
}

/**
 * 基于 HttpClient 的实现。使用【裸客户端】（无 Auth 插件）发起刷新请求，避免 Auth 刷新递归。
 */
class KtorTokenRefresher(
    private val client: HttpClient,
    private val baseUrlProvider: () -> String,
) : TokenRefresher {

    override suspend fun refresh(tokenType: String, refreshToken: String): LoginResponse? {
        return try {
            val response = client.post(baseUrlProvider() + "token/refresh") {
                header("Authorization", "$tokenType $refreshToken")
                contentType(ContentType.Application.Json)
            }
            when (val result = ResponseHandler.handleResponse<LoginResponse>(response)) {
                is NetworkResult.Success -> result.data
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
