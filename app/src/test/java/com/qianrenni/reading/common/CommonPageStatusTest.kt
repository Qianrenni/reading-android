package com.qianrenni.reading.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommonPageStatusTest {

    @Test
    fun `default is idle`() {
        val s = CommonPageStatus()
        assertFalse(s.isLoading)
        assertFalse(s.isError)
        assertNull(s.errorMessage)
    }

    @Test
    fun `loading sets loading flag`() {
        val s = CommonPageStatus().loading()
        assertTrue(s.isLoading)
        assertFalse(s.isError)
        assertNull(s.errorMessage)
    }

    @Test
    fun `down clears all flags`() {
        val s = CommonPageStatus().error("boom").down()
        assertFalse(s.isLoading)
        assertFalse(s.isError)
        assertNull(s.errorMessage)
    }

    @Test
    fun `error sets message and error flag`() {
        val s = CommonPageStatus().error("boom")
        assertTrue(s.isError)
        assertFalse(s.isLoading)
        assertEquals("boom", s.errorMessage)
    }
}
