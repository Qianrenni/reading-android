package com.qianrenni.reading.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * 构建 Ktor HttpClient 的工厂（无状态、可复用）。
 */
object HttpClientFactory {

    /**
     * 带 Bearer 认证（401 自动刷新令牌并重试）的客户端，用于普通 API 请求。
     *
     * @param loadTokens 每次请求读取当前令牌（无则返回 null，不携带 Authorization 头）
     * @param refreshTokens 收到 401 时刷新令牌（返回新令牌或 null）
     */
    fun createAuthClient(
        onLoadTokens: suspend () -> BearerTokens?,
        onRefreshTokens: suspend () -> BearerTokens?,
    ): HttpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
        install(Auth) {
            bearer {
                loadTokens { onLoadTokens() }
                refreshTokens { onRefreshTokens() }
            }
        }
        engine {
            connectTimeout = 30_000
            socketTimeout = 30_000
        }
    }

    /**
     * 无认证的裸客户端，用于令牌刷新等内部请求（避免 Auth 递归）。
     */
    fun createBareClient(): HttpClient = HttpClient(Android) {
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
