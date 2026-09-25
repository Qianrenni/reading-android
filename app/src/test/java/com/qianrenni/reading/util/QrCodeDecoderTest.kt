package com.qianrenni.reading.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 二维码解码测试：用 ZXing 自己生成真实二维码，再交给解码器识别。
 *
 * 这层是纯 JVM 逻辑（不含相机/CameraX），所以可以在单测里端到端验证解码，
 * 不需要真机或模拟器——release 包里的扫码能力因此有了可回归的保障。
 */
class QrCodeDecoderTest {

    private val payload = "gugareading://qr-login?ticket=QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo"

    /** 生成二维码亮度平面；[rowStrideExtra] 用于模拟相机帧的行填充。 */
    private fun renderQr(
        content: String,
        size: Int = 260,
        rowStrideExtra: Int = 0
    ): Triple<ByteArray, Int, Int> {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val stride = size + rowStrideExtra
        // 白底（0xFF）、黑点（0x00）
        val data = ByteArray(stride * size) { 0xFF.toByte() }
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (matrix.get(x, y)) {
                    data[y * stride + x] = 0x00
                }
            }
        }
        return Triple(data, stride, size)
    }

    @Test
    fun `紧凑亮度平面可解码`() {
        val (data, _, size) = renderQr(payload)

        assertEquals(payload, QrCodeDecoder.decodeYPlane(data, size, size))
    }

    @Test
    fun `带行填充的亮度平面可解码`() {
        // 相机帧的 Y 平面经常 rowStride > width，必须先按行裁剪再解码
        val (data, stride, size) = renderQr(payload, rowStrideExtra = 32)

        assertEquals(payload, QrCodeDecoder.decodeYPlane(data, size, size, rowStride = stride))
    }

    @Test
    fun `像素步长为 2 的亮度平面可解码`() {
        val size = 260
        val stride = size * 2
        val (source, _, _) = renderQr(payload, size = size)
        // 把紧凑数据按 pixelStride=2 展开（偶数位置有效，奇数位置为填充）
        val spread = ByteArray(stride * size) { 0x7F }
        for (y in 0 until size) {
            for (x in 0 until size) {
                spread[y * stride + x * 2] = source[y * size + x]
            }
        }

        assertEquals(
            payload,
            QrCodeDecoder.decodeYPlane(spread, size, size, rowStride = stride, pixelStride = 2)
        )
    }

    @Test
    fun `反色二维码也能识别`() {
        val size = 260
        val (data, _, _) = renderQr(payload, size = size)
        // 全画面取反：黑底白码（部分第三方生成的登录码就是深色底）
        val inverted = ByteArray(data.size) { (data[it].toInt() xor 0xFF).toByte() }

        assertEquals(payload, QrCodeDecoder.decodeYPlane(inverted, size, size))
    }

    @Test
    fun `无二维码的画面返回 null`() {
        val size = 200
        val blank = ByteArray(size * size) { 0xFF.toByte() }

        assertNull(QrCodeDecoder.decodeYPlane(blank, size, size))
    }

    @Test
    fun `参数非法时返回 null 而不是抛异常`() {
        val size = 100
        val data = ByteArray(size * size) { 0xFF.toByte() }

        // 宽高非法
        assertNull(QrCodeDecoder.decodeYPlane(data, 0, size))
        assertNull(QrCodeDecoder.decodeYPlane(data, size, 0))
        // 行填充小于宽度（不可能的数据布局）
        assertNull(QrCodeDecoder.decodeYPlane(data, size, size, rowStride = size - 1))
        // 数据长度不足
        assertNull(QrCodeDecoder.decodeYPlane(ByteArray(10), size, size))
        // 像素步长非法
        assertNull(QrCodeDecoder.decodeYPlane(data, size, size, pixelStride = 0))
    }

    @Test
    fun `扫描识别到的是二维码原文`() {
        // 解码器不做任何业务解析，票据的解析由 QrLoginPayload 负责
        val (data, _, size) = renderQr("https://example.com/anything")

        assertEquals("https://example.com/anything", QrCodeDecoder.decodeYPlane(data, size, size))
        assertNull(QrLoginPayload.parse(QrCodeDecoder.decodeYPlane(data, size, size)))
    }
}
