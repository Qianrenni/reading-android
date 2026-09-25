package com.qianrenni.reading.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * 二维码解码（ZXing core，纯 Java）。
 *
 * 不依赖 Android 框架与 CameraX，输入是原始亮度平面 —— 因此可以在 JVM 单测里
 * 用真实生成的二维码直接验证解码正确性，不必依赖相机/真机。
 *
 * 为什么不用 ML Kit：它通过清单里的字符串名反射加载组件、并依赖 native 库，
 * 在开启 R8 的 release 包里出现过 `BarcodeScanning.getClient()` 读到 null 而崩溃；
 * 而扫码登录是核心路径，纯 Java 的 ZXing 在 debug / release、有无 GMS 的设备上行为一致。
 */
object QrCodeDecoder {

    /**
     * 只认 QR Code，并开启 TRY_HARDER 提高倾斜、小尺寸码的识别率。
     *
     * 不用 `ALSO_INVERTED`：该 hint 对二维码解码路径不生效（实测反色码解不出来），
     * 反色由 [decodeYPlane] 自己判断后重试。
     */
    private val HINTS: Map<DecodeHintType, Any> = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
    )

    /** 整体亮度低于该值才值得再试一次反色解码（0~255）。 */
    private const val DARK_THRESHOLD = 110

    /**
     * 从相机 YUV_420_888 帧的 Y（亮度）平面解码二维码。
     *
     * @param yPlane Y 平面原始字节（可能含行填充 / 像素步长）
     * @param width 画面宽度（像素）
     * @param height 画面高度（像素）
     * @param rowStride Y 平面每行字节数（>= width）
     * @param pixelStride Y 平面相邻像素的字节间隔（通常 1，个别设备为 2）
     * @return 二维码文本；这一帧没解出来（或参数非法）返回 null，交给下一帧继续尝试
     */
    fun decodeYPlane(
        yPlane: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int = width,
        pixelStride: Int = 1,
    ): String? {
        val luminance = toPackedLuminance(yPlane, width, height, rowStride, pixelStride)
            ?: return null
        decode(luminance, width, height)?.let { return it }
        // 深色底白码（反色二维码）兜底：仅在画面整体偏暗时才多跑一轮，
        // 避免正常的浅色场景每帧多一次二值化开销
        if (!isDarkish(luminance)) {
            return null
        }
        return decode(invert(luminance), width, height)
    }

    private fun decode(luminance: ByteArray, width: Int, height: Int): String? {
        val source = PlanarYUVLuminanceSource(
            luminance, width, height, 0, 0, width, height, false
        )
        return try {
            // MultiFormatReader 有内部状态，每次新建以免跨帧/跨线程串状态
            MultiFormatReader().apply { setHints(HINTS) }
                .decode(BinaryBitmap(HybridBinarizer(source)))
                .text
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            // NotFoundException / ChecksumException / FormatException：本帧无有效二维码
            null
        }
    }

    private fun invert(luminance: ByteArray): ByteArray =
        ByteArray(luminance.size) { (luminance[it].toInt() xor 0xFF).toByte() }

    /** 抽样估算整体亮度（最多约 4096 个采样点，避免整帧遍历）。 */
    private fun isDarkish(luminance: ByteArray): Boolean {
        val step = maxOf(1, luminance.size / 4096)
        var sum = 0L
        var count = 0
        var index = 0
        while (index < luminance.size) {
            sum += luminance[index].toInt() and 0xFF
            count++
            index += step
        }
        return count > 0 && sum / count < DARK_THRESHOLD
    }

    /**
     * 把平面数据按行紧凑成 [width] × [height] 的亮度数组。
     *
     * CameraX 给出的 Y 平面常常带行填充（rowStride > width）或 2 字节步长，
     * 直接交给 ZXing 会因行错位而识别失败。
     */
    private fun toPackedLuminance(
        yPlane: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
    ): ByteArray? {
        if (width <= 0 || height <= 0) return null
        if (rowStride < width || pixelStride < 1) return null
        val required = (height - 1).toLong() * rowStride + (width - 1).toLong() * pixelStride + 1
        if (yPlane.size < required) return null

        val packed = ByteArray(width * height)
        if (rowStride == width && pixelStride == 1) {
            System.arraycopy(yPlane, 0, packed, 0, packed.size)
            return packed
        }
        var target = 0
        for (row in 0 until height) {
            var source = row * rowStride
            repeat(width) {
                packed[target++] = yPlane[source]
                source += pixelStride
            }
        }
        return packed
    }
}
