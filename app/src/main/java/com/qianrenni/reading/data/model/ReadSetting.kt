package com.qianrenni.reading.data.model

import androidx.compose.ui.text.font.FontFamily

/**
 * 阅读排版设置：字号 / 行高 / 字距 / 字体。
 *
 * 正文与纸张颜色由全应用统一的主题（`ThemeMode`）决定，不再单独保存，
 * 因此阅读页的配色与整个 App 完全一致。
 */
data class ReadSettings(
    val fontSize: Float = 18f,
    val lineHeight: Float = 30f,
    val letterSpacing: Float = 2f,
    val fontFamily: FontFamily = FontFamily.Default
)

enum class ReadFontFamily(val displayName: String, val value: FontFamily) {
    Default("默认", FontFamily.Default),
    Serif("宋体", FontFamily.Serif),
    SansSerif("黑体", FontFamily.SansSerif),
    Monospace("等宽", FontFamily.Monospace),
    Cursive("手写", FontFamily.Cursive)
}