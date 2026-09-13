package com.qianrenni.reading.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebUrlTest {

    @Test
    fun `normalizeWebUrl keeps http links and trims blanks`() {
        assertEquals(
            "http://49.235.107.221:8000/static/about.html",
            normalizeWebUrl("  http://49.235.107.221:8000/static/about.html  ")
        )
        assertEquals("https://example.com/a?b=1#c", normalizeWebUrl("https://example.com/a?b=1#c"))
    }

    @Test
    fun `normalizeWebUrl adds https to bare host`() {
        assertEquals("https://example.com", normalizeWebUrl("example.com"))
        assertEquals("https://example.com/activity?id=1", normalizeWebUrl("example.com/activity?id=1"))
        assertEquals("https://example.com:8443/x", normalizeWebUrl("example.com:8443/x"))
        assertEquals("https://localhost:8000/health", normalizeWebUrl("localhost:8000/health"))
        assertEquals("https://[::1]:8080/x", normalizeWebUrl("[::1]:8080/x"))
    }

    @Test
    fun `normalizeWebUrl resolves protocol relative link`() {
        assertEquals("https://example.com/x", normalizeWebUrl("//example.com/x"))
    }

    @Test
    fun `normalizeWebUrl rejects blank and relative paths`() {
        assertNull(normalizeWebUrl(""))
        assertNull(normalizeWebUrl("   "))
        assertNull(normalizeWebUrl("/activity/detail"))
        assertNull(normalizeWebUrl("./detail.html"))
    }

    @Test
    fun `normalizeWebUrl rejects non web schemes`() {
        assertNull(normalizeWebUrl("tel:10086"))
        assertNull(normalizeWebUrl("mailto:support@example.com"))
        assertNull(normalizeWebUrl("intent://scan/#Intent;scheme=zxing;end"))
        assertNull(normalizeWebUrl("weixin://dl/business"))
        assertNull(normalizeWebUrl("ftp://example.com/a.zip"))
        assertNull(normalizeWebUrl("http://"))
        assertNull(normalizeWebUrl("https:///path"))
    }

    @Test
    fun `isHttpUrl checks scheme and host`() {
        assertTrue(isHttpUrl("http://example.com"))
        assertTrue(isHttpUrl("https://example.com"))
        assertTrue(isHttpUrl("HTTPS://EXAMPLE.COM/a"))
        assertTrue(isHttpUrl("http://user:pass@example.com/x"))
        assertTrue(isHttpUrl("http://49.235.107.221:8000/static/x.html"))

        assertFalse(isHttpUrl("example.com"))
        assertFalse(isHttpUrl("ftp://example.com"))
        assertFalse(isHttpUrl("http://"))
        assertFalse(isHttpUrl("http:///path"))
        assertFalse(isHttpUrl("about:blank"))
        assertFalse(isHttpUrl(""))
    }

    @Test
    fun `shouldOpenInWebView keeps web and webview internal schemes inside`() {
        assertTrue(shouldOpenInWebView("https://example.com"))
        assertTrue(shouldOpenInWebView("about:blank"))
        assertTrue(shouldOpenInWebView("data:text/html,<h1>hi</h1>"))
        assertTrue(shouldOpenInWebView("blob:https://example.com/id"))
    }

    @Test
    fun `shouldOpenInWebView hands over external schemes to system apps`() {
        assertFalse(shouldOpenInWebView("tel:10086"))
        assertFalse(shouldOpenInWebView("mailto:support@example.com"))
        assertFalse(shouldOpenInWebView("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(shouldOpenInWebView("weixin://dl/business"))
        assertFalse(shouldOpenInWebView("market://details?id=com.qianrenni.reading"))
    }
}
