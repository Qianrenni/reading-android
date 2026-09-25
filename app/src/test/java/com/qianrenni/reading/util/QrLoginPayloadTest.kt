package com.qianrenni.reading.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 扫码登录二维码内容的编解码测试。
 * 约定需与后端 `com.qianrenni.modules.user.QrLoginPayload` 保持一致。
 */
class QrLoginPayloadTest {

    @Test
    fun `build 与 parse 互为逆运算`() {
        val payload = QrLoginPayload.build("abc123")

        assertEquals("gugareading://qr-login?ticket=abc123", payload)
        assertEquals("abc123", QrLoginPayload.parse(payload))
    }

    @Test
    fun `parse 忽略首尾空白`() {
        assertEquals("t1", QrLoginPayload.parse("  gugareading://qr-login?ticket=t1 \n"))
    }

    @Test
    fun `parse 忽略 scheme 与 host 大小写`() {
        assertEquals("t1", QrLoginPayload.parse("GugaReading://QR-Login?ticket=t1"))
    }

    @Test
    fun `parse 对非本系统二维码返回 null`() {
        assertNull(QrLoginPayload.parse("https://example.com/qr-login?ticket=t1"))
        assertNull(QrLoginPayload.parse("gugareading://other?ticket=t1"))
        assertNull(QrLoginPayload.parse("WIFI:S:mywifi;T:WPA;P:1234;;"))
    }

    @Test
    fun `parse 对裸票据与其他异常输入返回 null`() {
        // 裸票据字符串不是本系统的登录码——避免任意二维码文本被当成票据
        assertNull(QrLoginPayload.parse("abc123"))
        assertNull(QrLoginPayload.parse("gugareading://qr-login"))
        assertNull(QrLoginPayload.parse("gugareading://qr-login?ticket="))
        assertNull(QrLoginPayload.parse("gugareading://qr-login?ticket=   "))
        assertNull(QrLoginPayload.parse(""))
        assertNull(QrLoginPayload.parse("   "))
        assertNull(QrLoginPayload.parse(null))
    }

    @Test
    fun `parse 容忍票据中的额外参数`() {
        assertEquals(
            "t1",
            QrLoginPayload.parse("gugareading://qr-login?from=web&ticket=t1")
        )
    }
}
