package com.qianrenni.reading.ui.theme

import androidx.compose.ui.graphics.Color
import com.qianrenni.reading.data.repository.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 阅读页配色必须由全应用统一的主题推导，保证「阅读主题 == App 主题」。
 */
class ReadingPaletteTest {

    private val presets = listOf(
        ThemeMode.LIGHT,
        ThemeMode.DARK,
        ThemeMode.GREEN,
        ThemeMode.PARCHMENT,
        ThemeMode.WOOD
    )

    @Test
    fun `system mode follows system dark`() {
        assertEquals(
            readingPalette(ThemeMode.LIGHT, isSystemDark = false),
            readingPalette(ThemeMode.SYSTEM, isSystemDark = false)
        )
        assertEquals(
            readingPalette(ThemeMode.DARK, isSystemDark = true),
            readingPalette(ThemeMode.SYSTEM, isSystemDark = true)
        )
    }

    @Test
    fun `every preset theme has its own paper color`() {
        val backgrounds = presets.map { readingPalette(it, isSystemDark = false).background }

        assertEquals("5 套主题的纸张色应互不相同", 5, backgrounds.toSet().size)
    }

    @Test
    fun `text color is readable against paper color`() {
        presets.forEach { mode ->
            val palette = readingPalette(mode, isSystemDark = false)
            assertNotEquals("$mode 正文色不能与纸张色相同", palette.background, palette.text)
        }
    }

    @Test
    fun `green theme uses eye care colors`() {
        val green = readingPalette(ThemeMode.GREEN, isSystemDark = false)

        assertEquals(Color(0xFFC7EDCC), green.background)
        assertEquals(PaperTexture.NONE, green.texture)
    }

    @Test
    fun `texture themes declare texture and gradient`() {
        val parchment = readingPalette(ThemeMode.PARCHMENT, isSystemDark = false)
        assertEquals(PaperTexture.PAPER, parchment.texture)
        assertTrue(parchment.gradient.size > 1)

        val wood = readingPalette(ThemeMode.WOOD, isSystemDark = false)
        assertEquals(PaperTexture.WOOD, wood.texture)
        assertTrue(wood.gradient.size > 1)
    }

    @Test
    fun `texture seed is stable for the same theme`() {
        assertEquals(
            readingPalette(ThemeMode.PARCHMENT, isSystemDark = false).textureSeed,
            readingPalette(ThemeMode.PARCHMENT, isSystemDark = true).textureSeed
        )
    }

    @Test
    fun `material color scheme stays in sync with reading palette`() {
        listOf(true, false).forEach { systemDark ->
            ThemeMode.entries.forEach { mode ->
                val scheme = colorSchemeOf(mode, isSystemDark = systemDark)
                val palette = readingPalette(mode, isSystemDark = systemDark)

                assertEquals(
                    "$mode 的阅读纸张色应等于主题 background",
                    scheme.background,
                    palette.background
                )
                assertEquals(
                    "$mode 的阅读正文色应等于主题 onBackground",
                    scheme.onBackground,
                    palette.text
                )
            }
        }
    }
}
