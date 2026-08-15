package com.qianrenni.reading.data.remote

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkResultTest {

    @Test
    fun `fold invokes success`() = runTest {
        var out = ""
        NetworkResult.Success(5).fold(onSuccess = { out = "s$it" })
        assertEquals("s5", out)
    }

    @Test
    fun `fold invokes failure`() = runTest {
        var out = ""
        NetworkResult.Failure("err", 400).fold(
            onFailure = { msg, code, _ -> out = "$msg:$code" }
        )
        assertEquals("err:400", out)
    }

    @Test
    fun `fold invokes empty`() = runTest {
        var called = false
        NetworkResult.Empty().fold(onEmpty = { called = true })
        assertTrue(called)
    }

    @Test
    fun `onSuccess maps only success`() = runTest {
        assertEquals(6, NetworkResult.Success(3).onSuccess { it * 2 })
        assertNull(NetworkResult.Failure("e").onSuccess { 1 })
        assertNull(NetworkResult.Empty().onSuccess { 1 })
    }

    @Test
    fun `onFailure maps only failure`() = runTest {
        val out = NetworkResult.Failure("err", 400).onFailure { m, c, _ -> "$m:$c" }
        assertEquals("err:400", out)
        assertNull(NetworkResult.Success(1).onFailure { _, _, _ -> "x" })
    }

    @Test
    fun `onEmpty runs only for empty`() = runTest {
        var called = false
        NetworkResult.Empty().onEmpty { called = true }
        assertTrue(called)
        NetworkResult.Success(1).onEmpty { called = false }
        assertTrue(called)
    }
}
