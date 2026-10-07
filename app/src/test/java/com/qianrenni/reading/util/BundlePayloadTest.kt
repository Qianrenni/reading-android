package com.qianrenni.reading.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 页面包二维码解析测试。
 *
 * 二维码是外部输入，这里重点验证「陌生码一律拒绝」：不认识的 scheme/host、缺字段、
 * 版本非法、校验和格式不对都不能进入下载流程（下载地址由服务端按 appKey+version 下发，
 * 二维码本身不含 URL，避免被引导去下载外部代码）。
 */
class BundlePayloadTest {

    private val md5 = "0123456789abcdef0123456789abcdef"
    private val expected = "gugareading://bundle?appKey=demo&version=12&md5=$md5"

    @Test
    fun `build 与 parse 互为逆运算`() {
        assertEquals(expected, BundlePayload.build(appKey = "demo", versionCode = 12, md5 = md5))

        val parsed = BundlePayload.parse(expected)
        assertEquals("demo", parsed?.appKey)
        assertEquals(12, parsed?.versionCode)
        assertEquals(md5, parsed?.md5)
    }

    @Test
    fun `scheme 与 host 及参数名大小写不敏感且允许前后空白`() {
        val parsed = BundlePayload.parse("  GUGAREADING://BUNDLE?APPKEY=demo&VERSION=3&MD5=$md5  ")
        assertEquals("demo", parsed?.appKey)
        assertEquals(3, parsed?.versionCode)
    }

    @Test
    fun `多余参数被忽略`() {
        val parsed = BundlePayload.parse("$expected&ts=1700000000&from=console")
        assertEquals("demo", parsed?.appKey)
    }

    @Test
    fun `缺少版本或校验和时拒绝解析`() {
        assertNull(BundlePayload.parse("gugareading://bundle?appKey=demo&md5=$md5"))
        assertNull(BundlePayload.parse("gugareading://bundle?appKey=demo&version=12"))
    }

    @Test
    fun `非法版本号被拒绝`() {
        assertNull(BundlePayload.parse("gugareading://bundle?appKey=demo&version=0&md5=$md5"))
        assertNull(BundlePayload.parse("gugareading://bundle?appKey=demo&version=-1&md5=$md5"))
        assertNull(BundlePayload.parse("gugareading://bundle?appKey=demo&version=abc&md5=$md5"))
    }

    @Test
    fun `非法校验和被拒绝`() {
        assertNull(BundlePayload.parse("gugareading://bundle?appKey=demo&version=1&md5=1234"))
        assertNull(
            BundlePayload.parse("gugareading://bundle?appKey=demo&version=1&md5=zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz")
        )
    }

    @Test
    fun `缺少 appKey 被拒绝`() {
        assertNull(BundlePayload.parse("gugareading://bundle?version=1&md5=$md5"))
        assertNull(BundlePayload.parse("gugareading://bundle?appKey=%20&version=1&md5=$md5"))
        assertNull(BundlePayload.parse("gugareading://bundle?appKey=&version=1&md5=$md5"))
    }

    @Test
    fun `appKey 只允许后端同意的字符集并统一为小写`() {
        assertEquals("demo-1_2", BundlePayload.parse(BundlePayload.build("Demo-1_2", 1, md5))?.appKey)
        assertNull(BundlePayload.parse("gugareading://bundle?appKey=-bad&version=1&md5=$md5"))
        assertNull(BundlePayload.parse("gugareading://bundle?appKey=a%2Fb&version=1&md5=$md5"))
    }

    @Test
    fun `非本系统的码一律拒绝`() {
        assertNull(BundlePayload.parse(null))
        assertNull(BundlePayload.parse(""))
        assertNull(BundlePayload.parse("https://example.com/?appKey=demo&version=1&md5=$md5"))
        assertNull(BundlePayload.parse("gugareading://login?ticket=abc"))
        assertNull(BundlePayload.parse("gugareading://bundle"))
        assertNull(BundlePayload.parse("demo"))
    }

    @Test
    fun `登录码不会被当成页面包码`() {
        assertNull(BundlePayload.parse(QrLoginPayload.build("ticket-1")))
    }

    @Test
    fun `页面包码不会被当成登录码`() {
        assertNull(QrLoginPayload.parse(expected))
    }
}
