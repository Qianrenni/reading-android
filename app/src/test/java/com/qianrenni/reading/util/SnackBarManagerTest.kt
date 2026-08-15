package com.qianrenni.reading.util

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SnackBarManagerTest {

    @Test
    fun `showMessage emits to messages`() = runTest {
        val job = launch { SnackBarManager.showMessage("hello") }
        val msg = SnackBarManager.messages.first()
        assertEquals("hello", msg)
        job.join()
    }
}
