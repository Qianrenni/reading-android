package com.qianrenni.reading.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 手机端对扫码票据的处置动作（与后端 `QrLoginAction` 对应）。 */
@Serializable
enum class QrLoginAction {
    @SerialName("scan")
    SCAN,

    @SerialName("confirm")
    CONFIRM,

    @SerialName("cancel")
    CANCEL,
}

/** 票据状态（与后端 `QrLoginStatus` 对应）。 */
@Serializable
enum class QrLoginStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("scanned")
    SCANNED,

    @SerialName("confirmed")
    CONFIRMED,

    @SerialName("canceled")
    CANCELED,
}

@Serializable
data class QrLoginActionRequest(
    val ticket: String,
    val action: QrLoginAction
)

@Serializable
data class QrLoginStatusResponse(
    val status: QrLoginStatus
)
