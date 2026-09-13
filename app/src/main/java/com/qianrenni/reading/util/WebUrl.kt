package com.qianrenni.reading.util

/**
 * H5（WebView）链接工具。
 *
 * 全部为纯 Kotlin 实现（不依赖 `android.net.Uri`），因此可直接单元测试。
 */

/** 裸域名 / IP（可带端口与路径），如 `example.com`、`a.b.com:8080/x`。 */
private val BARE_HOST_WITH_DOT = Regex("""^[\w\-]+(?:\.[\w\-]+)+(?::\d{1,5})?(?:[/?#].*)?$""")

/** 本机地址，如 `localhost:8000`。 */
private val BARE_LOCALHOST = Regex("""^localhost(?::\d{1,5})?(?:[/?#].*)?$""", RegexOption.IGNORE_CASE)

/** 裸 IPv6，如 `[::1]:8080/x`。 */
private val BARE_IPV6_HOST = Regex("""^\[[0-9a-fA-F:]+](?::\d{1,5})?(?:[/?#].*)?$""")

/** `scheme:` 前缀，如 `tel:`、`mailto:`、`intent:`。 */
private val SCHEME_PREFIX = Regex("""^[a-zA-Z][a-zA-Z0-9+.\-]*:""")

/** WebView 自身即可处理的 scheme，这类链接不应交给外部应用。 */
private val WEBVIEW_SCHEMES = setOf("about", "data", "blob", "javascript", "chrome")

/** 是否为 WebView 可直接加载的 http/https 链接（要求 host 非空）。 */
fun isHttpUrl(url: String): Boolean {
    val trimmed = url.trim()
    if (!trimmed.startsWith("http://", ignoreCase = true) &&
        !trimmed.startsWith("https://", ignoreCase = true)
    ) {
        return false
    }
    return hostOf(trimmed) != null
}

/** 是否为 WebView 自有 scheme（`about:`、`data:`、`blob:` 等）。 */
fun isWebViewInternalScheme(url: String): Boolean = schemeOf(url.trim()) in WEBVIEW_SCHEMES

/**
 * 链接是否应在应用内的 WebView 打开。
 * 返回 false 时应交给系统应用处理（系统浏览器、拨号、邮件等）。
 */
fun shouldOpenInWebView(url: String): Boolean = isHttpUrl(url) || isWebViewInternalScheme(url)

/**
 * 规范化外部传入的链接，使其可直接交给 WebView 加载：
 * - 去掉首尾空白；空串返回 `null`；
 * - `//host/path`（协议相对链接）补 `https:`；
 * - 裸域名 / IP（可带端口、路径，如 `example.com`、`localhost:8000`）补 `https://`；
 * - http/https 链接校验 host 非空；
 * - 其它 scheme（`tel:`、`mailto:`、`intent:` 等）与相对路径返回 `null`，
 *   这类链接应改用 `openWithSystem` 交给系统应用。
 */
fun normalizeWebUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    if (isHttpUrl(trimmed)) return trimmed
    if (trimmed.startsWith("//")) return normalizeWebUrl("https:$trimmed")
    if (isBareHost(trimmed)) return normalizeWebUrl("https://$trimmed")
    return null
}

/** 是否为「裸地址」（未带 scheme 的域名 / IP / localhost，可带端口与路径）。 */
private fun isBareHost(value: String): Boolean = BARE_HOST_WITH_DOT.matches(value) ||
    BARE_LOCALHOST.matches(value) ||
    BARE_IPV6_HOST.matches(value)

/** 取链接的 scheme（小写，不含冒号）；无 scheme 时返回空串。 */
private fun schemeOf(url: String): String {
    if (url.startsWith("//")) return ""
    return SCHEME_PREFIX.find(url)?.value?.removeSuffix(":")?.lowercase().orEmpty()
}

/** 取 `scheme://authority/...` 中的 host（去掉 userInfo 与端口），非法时返回 null。 */
private fun hostOf(url: String): String? {
    val separator = url.indexOf("://")
    if (separator <= 0) return null
    val authority = url.substring(separator + 3)
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('@')
    if (authority.isEmpty()) return null
    val host = if (authority.startsWith("[")) {
        authority.substringBefore(']').removePrefix("[")
    } else {
        authority.substringBefore(':')
    }
    return host.ifBlank { null }
}
