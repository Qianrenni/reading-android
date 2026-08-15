package com.qianrenni.reading.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * 构建 Ktor HttpClient 的工厂（无状态、可复用）。
 */
object HttpClientFactory {

    fun create(): HttpClient = HttpClient(Android) {
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
