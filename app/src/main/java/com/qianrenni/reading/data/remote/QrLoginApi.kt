package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.QrIdRequest
import com.qianrenni.reading.data.model.QrScanResponse
import io.ktor.client.request.setBody

/**
 * 扫码登录相关 API（均要求已登录，Authorization 由 Ktor Bearer 插件自动注入）。
 */
interface QrLoginApi {
    /** 上报扫码，二维码进入“已扫描”状态，返回网页端描述 */
    suspend fun scan(request: QrIdRequest): NetworkResult<QrScanResponse>

    /** 确认登录，网页端将拿到一次性兑换凭证换取令牌 */
    suspend fun confirm(request: QrIdRequest): NetworkResult<Unit>

    /** 取消本次登录 */
    suspend fun cancel(request: QrIdRequest): NetworkResult<Unit>
}

class QrLoginApiImpl(private val apiClient: ApiClient) : QrLoginApi {

    override suspend fun scan(request: QrIdRequest): NetworkResult<QrScanResponse> {
        return apiClient.post("token/qr/scan") {
            setBody(request)
        }
    }

    override suspend fun confirm(request: QrIdRequest): NetworkResult<Unit> {
        return apiClient.post("token/qr/confirm") {
            setBody(request)
        }
    }

    override suspend fun cancel(request: QrIdRequest): NetworkResult<Unit> {
        return apiClient.post("token/qr/cancel") {
            setBody(request)
        }
    }
}
