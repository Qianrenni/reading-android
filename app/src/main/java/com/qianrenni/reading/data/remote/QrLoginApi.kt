package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.QrLoginAction
import com.qianrenni.reading.data.model.QrLoginActionRequest
import com.qianrenni.reading.data.model.QrLoginStatusResponse
import io.ktor.client.request.setBody

/**
 * 扫码登录 API（手机端）。
 *
 * 只有「上报动作」一个接口：后端用 `action` 区分扫描 / 确认 / 取消，
 * 语义化方法由 [com.qianrenni.reading.viewmodels.qr.QrScanViewModel] 提供。
 * 该接口需要登录态（Authorization 由 Ktor Auth 插件自动注入），
 * 后端据此记录「是谁在授权这次网页登录」。
 */
interface QrLoginApi {
    suspend fun sendAction(
        ticket: String,
        action: QrLoginAction
    ): NetworkResult<QrLoginStatusResponse>
}

class QrLoginApiImpl(private val apiClient: ApiClient) : QrLoginApi {

    override suspend fun sendAction(
        ticket: String,
        action: QrLoginAction
    ): NetworkResult<QrLoginStatusResponse> {
        return apiClient.post("token/qr/action") {
            setBody(QrLoginActionRequest(ticket = ticket, action = action))
        }
    }
}
