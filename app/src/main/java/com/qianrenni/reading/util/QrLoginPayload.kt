package com.qianrenni.reading.util

/**
 * 扫码登录二维码内容的编解码。
 *
 * 约定（与后端 `com.qianrenni.modules.user.QrLoginPayload` 保持一致）：
 * `gugareading://qr-login?ticket=<ticket>`。
 *
 * 用自定义 scheme 承载票据，而不是把裸票据字符串印进二维码——
 * 扫码后能明确区分「本系统网页端登录页的二维码」与任意二维码文本。
 *
 * 手工解析而非用 `android.net.Uri`：本类要能在 JVM 单元测试里直接验证，
 * 而 `Uri` 在非仪器化测试中只有桩实现。
 *
 * 不做百分号解码：票据由后端按 URL 安全字符集（base64url）生成，
 * 不会出现需要解码的字符，多一步解码反而会把畸形输入放行。
 */
object QrLoginPayload {
    const val SCHEME = "gugareading"
    const val HOST = "qr-login"
    const val PARAM_TICKET = "ticket"

    private const val SCHEME_SEPARATOR = "://"
    private const val PARAM_SEPARATOR = '&'
    private const val VALUE_SEPARATOR = '='

    /** 构造二维码内容（仅测试与文档使用，实际内容由后端下发）。 */
    fun build(ticket: String): String = "$SCHEME$SCHEME_SEPARATOR$HOST?$PARAM_TICKET=$ticket"

    /**
     * 从扫码结果中解析票据。
     *
     * @return 票据字符串；不是本系统的登录码、或票据为空时返回 null
     */
    fun parse(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        val schemeEnd = text.indexOf(SCHEME_SEPARATOR)
        if (schemeEnd <= 0) return null
        if (!text.substring(0, schemeEnd).equals(SCHEME, ignoreCase = true)) return null

        val rest = text.substring(schemeEnd + SCHEME_SEPARATOR.length)
        val queryStart = rest.indexOf('?')
        if (queryStart < 0) return null
        if (!rest.substring(0, queryStart).equals(HOST, ignoreCase = true)) return null

        return rest.substring(queryStart + 1)
            .split(PARAM_SEPARATOR)
            .firstNotNullOfOrNull { param -> ticketValue(param) }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    /** 取出 `ticket=<value>` 形式参数的值；不是该参数时返回 null。 */
    private fun ticketValue(param: String): String? {
        val separator = param.indexOf(VALUE_SEPARATOR)
        if (separator <= 0) return null
        if (!param.substring(0, separator).equals(PARAM_TICKET, ignoreCase = true)) return null
        return param.substring(separator + 1)
    }
}
