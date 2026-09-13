package com.qianrenni.reading.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.qianrenni.reading.data.repository.ThemeMode
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** 阅读页纸张质感类型。 */
enum class PaperTexture { NONE, PAPER, WOOD }

/**
 * 阅读页配色：正文色 / 纸张底色 / 质感。
 *
 * 由全应用统一的 [ThemeMode] 推导（[readingPalette]），因此阅读页与整个 App 主题一致。
 */
data class ReadingPalette(
    val text: Color,
    val secondaryText: Color,
    val background: Color,
    /** 多段时作为纸张底色渐变（比纯色更有层次）；为空则用 [background] 纯色。 */
    val gradient: List<Color> = emptyList(),
    val texture: PaperTexture = PaperTexture.NONE,
    /** 质感绘制用固定种子，保证同一主题每帧结果一致、不闪烁。 */
    val textureSeed: Int = 0
)

/** 主题 → 阅读页配色（[ThemeMode.SYSTEM] 跟随系统深浅色）。 */
fun readingPalette(themeMode: ThemeMode, isSystemDark: Boolean): ReadingPalette = when (themeMode) {
    ThemeMode.SYSTEM -> if (isSystemDark) DARK_PALETTE else LIGHT_PALETTE
    ThemeMode.LIGHT -> LIGHT_PALETTE
    ThemeMode.DARK -> DARK_PALETTE
    ThemeMode.GREEN -> GREEN_PALETTE
    ThemeMode.PARCHMENT -> PARCHMENT_PALETTE
    ThemeMode.WOOD -> WOOD_PALETTE
}

private val LIGHT_PALETTE = ReadingPalette(
    text = Color(0xFF201A15),
    secondaryText = Color(0xFF52443A),
    background = Color(0xFFFDF6EF)
)

private val DARK_PALETTE = ReadingPalette(
    text = Color(0xFFEDE0D4),
    secondaryText = Color(0xFFD8C2B0),
    background = Color(0xFF1A130D)
)

/** 绿色护眼：经典护眼底色 + 深墨绿正文。 */
private val GREEN_PALETTE = ReadingPalette(
    text = Color(0xFF2D4A2D),
    secondaryText = Color(0xFF47634A),
    background = Color(0xFFC7EDCC),
    gradient = listOf(Color(0xFFCDEFD2), Color(0xFFBFE6C6))
)

/** 羊皮纸：米黄纸张 + 纤维质感（真实纸张的细颗粒与纤维）。 */
private val PARCHMENT_PALETTE = ReadingPalette(
    text = Color(0xFF5B4636),
    secondaryText = Color(0xFF7A6350),
    background = Color(0xFFF5F0E1),
    gradient = listOf(Color(0xFFF9F4E7), Color(0xFFF1E9D6), Color(0xFFF6F1E3)),
    texture = PaperTexture.PAPER,
    textureSeed = 20260913
)

/** 木制家具：深木色底 + 木纹纹理 + 暖白正文。 */
private val WOOD_PALETTE = ReadingPalette(
    text = Color(0xFFF2E6D4),
    secondaryText = Color(0xFFD8C2A6),
    background = Color(0xFF33241A),
    gradient = listOf(Color(0xFF3F2E20), Color(0xFF2C1E14)),
    texture = PaperTexture.WOOD,
    textureSeed = 7219
)

/**
 * 阅读页背景：纯色 / 柔和渐变，[PaperTexture.PAPER] 叠加纸张纤维、[PaperTexture.WOOD] 叠加木纹。
 *
 * 绘制内容随尺寸缓存（[drawWithCache]），种子固定，不会每帧抖动。
 */
fun Modifier.readingBackground(palette: ReadingPalette): Modifier = drawWithCache {
    val grains = when (palette.texture) {
        PaperTexture.NONE -> emptyList()
        PaperTexture.PAPER -> paperGrains(size, palette.textureSeed)
        PaperTexture.WOOD -> woodGrains(size, palette.textureSeed)
    }
    val brush = if (palette.gradient.size > 1) Brush.linearGradient(palette.gradient) else null
    onDrawBehind {
        if (brush != null) drawRect(brush) else drawRect(palette.background)
        grains.forEach { grain ->
            drawLine(
                color = grain.color,
                start = Offset(grain.x, grain.y),
                end = Offset(grain.x2, grain.y2),
                strokeWidth = grain.width,
                cap = StrokeCap.Round
            )
        }
    }
}

/** 质感纹理中的一条纤维 / 一道木纹。 */
private class Grain(
    val x: Float,
    val y: Float,
    val x2: Float,
    val y2: Float,
    val width: Float,
    val color: Color
)

/** 纸张纤维：细短、低对比的深浅线条，叠加后形成纸张颗粒感。 */
private fun paperGrains(size: Size, seed: Int): List<Grain> {
    if (size.width <= 0f || size.height <= 0f) return emptyList()
    val random = Random(seed)
    return List(240) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        val length = 6f + random.nextFloat() * 26f
        val angle = (random.nextFloat() - 0.5f) * 1.2f
        Grain(
            x = x,
            y = y,
            x2 = x + length * cos(angle),
            y2 = y + length * sin(angle),
            width = 0.6f + random.nextFloat() * 1.4f,
            color = if (random.nextBoolean()) {
                Color(0xFF6B5B45).copy(alpha = 0.05f)
            } else {
                Color(0xFFFFFDF5).copy(alpha = 0.55f)
            }
        )
    }
}

/** 木纹：纵向深浅不一的细长条纹，叠加后形成家具木纹质感。 */
private fun woodGrains(size: Size, seed: Int): List<Grain> {
    if (size.width <= 0f || size.height <= 0f) return emptyList()
    val random = Random(seed)
    return List(72) {
        val x = random.nextFloat() * size.width
        Grain(
            x = x,
            y = 0f,
            x2 = x + 3f - random.nextFloat() * 6f,
            y2 = size.height,
            width = 1.5f + random.nextFloat() * 5f,
            color = if (random.nextBoolean()) {
                Color(0xFF1E1310).copy(alpha = 0.30f)
            } else {
                Color(0xFF6B4A2E).copy(alpha = 0.22f)
            }
        )
    }
}
