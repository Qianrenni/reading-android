package com.qianrenni.reading.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrLoginParserTest {

    @Test
    fun `标准协议内容解析出 qrId`() {
        val token = QrLoginParser.parseToken("guga://qr-login?token=abc123XYZ")
        assertEquals("abc123XYZ", token)
    }

    @Test
    fun `非本应用协议内容返回 null`() {
        assertNull(QrLoginParser.parseToken("https://example.com/qr?token=abc"))
        assertNull(QrLoginParser.parseToken("guga://other?token=abc"))
        assertNull(QrLoginParser.parseToken("wxp://f2f0xCn123"))
    }

    @Test
    fun `空内容返回 null`() {
        assertNull(QrLoginParser.parseToken(null))
        assertNull(QrLoginParser.parseToken(""))
        assertNull(QrLoginParser.parseToken("   "))
    }

    @Test
    fun `协议前缀后为空或纯空白返回 null`() {
        assertNull(QrLoginParser.parseToken("guga://qr-login?token="))
        assertNull(QrLoginParser.parseToken("guga://qr-login?token=   "))
    }

    @Test
    fun `token 含协议保留字符时原样返回不截断`() {
        val token = QrLoginParser.parseToken("guga://qr-login?token=a-b_C")
        assertEquals("a-b_C", token)
    }
}
