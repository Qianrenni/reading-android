package com.qianrenni.reading.navigation

import com.qianrenni.reading.util.normalizeWebUrl

/**
 * 在应用内 H5 容器（WebView）中打开链接。
 *
 * 链接会先经 [normalizeWebUrl] 规范化：自动去掉首尾空白、补全 `https://`；
 * 无法作为网页加载的链接（`tel:`、`mailto:`、`intent:`、相对路径等）会被忽略并返回 false，
 * 这类链接请改用 `com.qianrenni.reading.util.openWithSystem` 交由系统应用处理。
 *
 * 用法：`navigator.openWebPage("example.com/activity", title = "活动详情")`
 *
 * @return 是否已导航到 H5 页面。
 */
fun Navigator.openWebPage(url: String, title: String? = null): Boolean {
    val normalized = normalizeWebUrl(url) ?: return false
    navigate(WebPage(url = normalized, title = title))
    return true
}
