package com.qianrenni.reading.util

/**
 * 扫码登录二维码内容解析。
 *
 * 网页端二维码内容为 `guga://qr-login?token=<qrId>`，
 * 带协议前缀可避免把用户扫到的无关二维码（普通网址、其他小程序码等）
 * 当作登录票据提交。
 */
object QrLoginParser {

    /** 与网页端 composable 中的 QR_LOGIN_SCHEME 保持一致 */
    const val QR_LOGIN_SCHEME = "guga://qr-login?token="

    /**
     * 从二维码原始文本中解析登录票据 qrId。
     * @return 合法的登录票据；非本应用协议内容返回 null
     */
    fun parseToken(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        if (!raw.startsWith(QR_LOGIN_SCHEME)) return null
        val token = raw.removePrefix(QR_LOGIN_SCHEME).trim()
        return token.ifBlank { null }
    }
}
