package com.qianrenni.reading.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ToolTest {

    @Test
    fun `indexToCN converts zero to empty`() {
        assertEquals("", indexToCN(0))
    }

    @Test
    fun `indexToCN converts single digit`() {
        assertEquals("一", indexToCN(1))
        assertEquals("五", indexToCN(5))
        assertEquals("九", indexToCN(9))
    }

    @Test
    fun `indexToCN converts ten`() {
        assertEquals("十", indexToCN(10))
    }

    @Test
    fun `indexToCN converts teens`() {
        assertEquals("十一", indexToCN(11))
        assertEquals("十五", indexToCN(15))
        assertEquals("十九", indexToCN(19))
    }

    @Test
    fun `indexToCN converts tens`() {
        assertEquals("二十", indexToCN(20))
        assertEquals("九十", indexToCN(90))
    }

    @Test
    fun `indexToCN converts hundreds`() {
        assertEquals("一百", indexToCN(100))
        assertEquals("一百零一", indexToCN(101))
        assertEquals("一百二十", indexToCN(120))
        assertEquals("九百九十九", indexToCN(999))
    }

    @Test
    fun `indexToCN rejects negative`() {
        assertThrows(IllegalArgumentException::class.java) { indexToCN(-1) }
    }
}
