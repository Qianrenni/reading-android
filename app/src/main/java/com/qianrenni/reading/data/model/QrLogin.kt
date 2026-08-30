package com.qianrenni.reading.data.model

import kotlinx.serialization.Serializable

/** 扫码登录:App 上报扫描/确认/取消的二维码票据 ID */
@Serializable
data class QrIdRequest(
    val qrId: String
)

/** 扫码登录:scan 响应,携带网页端描述供确认框展示 */
@Serializable
data class QrScanResponse(
    val client: String? = null
)
