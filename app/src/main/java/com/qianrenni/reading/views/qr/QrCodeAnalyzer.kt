package com.qianrenni.reading.views.qr

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.qianrenni.reading.util.QrCodeDecoder

/**
 * 相机帧 → 二维码文本的胶水层：只负责取 Y 平面、调用纯逻辑解码器 [QrCodeDecoder]，
 * 解码算法本身（含各种异常输入的处理）在 `util/QrCodeDecoder.kt` 中单测覆盖。
 *
 * [enabled] 由界面在「已识别到有效票据」后置 false——相机继续预览，但不再做无谓的解码。
 */
class QrCodeAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    @Volatile
    var enabled: Boolean = true

    override fun analyze(imageProxy: ImageProxy) {
        try {
            if (!enabled) {
                return
            }
            val yPlane = imageProxy.planes.firstOrNull() ?: return
            val buffer = yPlane.buffer
            // 按剩余长度取，避免把 buffer 的 position/limit 之外的内容读进来
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            val text = QrCodeDecoder.decodeYPlane(
                yPlane = data,
                width = imageProxy.width,
                height = imageProxy.height,
                rowStride = yPlane.rowStride,
                pixelStride = yPlane.pixelStride,
            )
            if (text != null) {
                onQrDetected(text)
            }
        } finally {
            // 必须关闭，否则后续帧会被 STRATEGY_KEEP_ONLY_LATEST 丢弃
            imageProxy.close()
        }
    }
}
