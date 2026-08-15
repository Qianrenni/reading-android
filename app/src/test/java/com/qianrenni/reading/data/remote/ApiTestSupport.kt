package com.qianrenni.reading.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val TEST_BASE_URL = "http://test/"

/** 生成 JSON 响应（需在 MockRequestHandleScope 上下文中调用）。 */
fun MockRequestHandleScope.jsonBody(body: String, status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

/** 生成空响应（如 204）。 */
fun MockRequestHandleScope.emptyBody(status: HttpStatusCode = HttpStatusCode.NoContent): HttpResponseData =
    respond("", status)

/** 读取请求体文本（ContentNegotiation 序列化后的 TextContent）。 */
fun io.ktor.client.request.HttpRequestData.bodyText(): String =
    (body as? io.ktor.http.content.TextContent)?.text ?: ""

/**
 * 构建一个基于 MockEngine 的 ApiClient 测试夹具。
 * handler 为最后一个参数，便于使用尾随 lambda。
 */
fun buildTestApiClient(
    baseUrlProvider: () -> String = { TEST_BASE_URL },
    onUnauthorized: () -> Unit = {},
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): ApiClient {
    val engine = MockEngine { handler(it) }
    val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    return ApiClient(client, baseUrlProvider, onUnauthorized)
}
