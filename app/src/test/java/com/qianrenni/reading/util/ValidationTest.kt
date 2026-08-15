package com.qianrenni.reading.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTest {

    @Test
    fun `valid emails pass`() {
        assertTrue(isValidEmail("a@b.c"))
        assertTrue(isValidEmail("user.name+tag@sub.domain.com"))
        assertTrue(isValidEmail("tom@e.c"))
    }

    @Test
    fun `invalid emails fail`() {
        assertFalse(isValidEmail(""))
        assertFalse(isValidEmail("bad"))
        assertFalse(isValidEmail("a@b"))
        assertFalse(isValidEmail("a b@c.d"))
        assertFalse(isValidEmail("@c.d"))
    }
}
