package com.qianrenni.reading.data.model

import androidx.compose.ui.text.font.FontFamily

enum class Themes(
    val label: String,
    val textColor: Int,
    val backgroundColor: Int
) {
    EyeTheme(
        label = "护眼",
        textColor = 0xff2d4a2d.toInt(),
        backgroundColor = 0xffc7edcc.toInt()
    ),
    PaperTheme(
        label = "纹理",
        textColor = 0xff5b4636.toInt(),
        backgroundColor = 0xfff5f0e1.toInt()
    )
}

data class ReadSettings(
    val fontSize: Float = 18f,
    val lineHeight: Float = 30f,
    val letterSpacing: Float = 2f,
    val fontFamily: FontFamily = FontFamily.Default,
    val textColor: Int,
    val backgroundColor: Int
) {

}

enum class ReadFontFamily(val displayName: String, val value: FontFamily) {
    Default("默认", FontFamily.Default),
    Serif("宋体", FontFamily.Serif),
    SansSerif("黑体", FontFamily.SansSerif),
    Monospace("等宽", FontFamily.Monospace),
    Cursive("手写", FontFamily.Cursive)
}