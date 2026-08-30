package com.qianrenni.reading.viewmodels.auth

import com.qianrenni.reading.data.model.QrIdRequest
import com.qianrenni.reading.data.model.QrScanResponse
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.data.remote.QrLoginApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QrLoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    /** 内存版 QrLoginApi：按成员变量决定各接口返回结果，并记录调用 */
    private class FakeQrLoginApi(
        var scanResult: NetworkResult<QrScanResponse> = NetworkResult.Empty(),
        var confirmResult: NetworkResult<Unit> = NetworkResult.Empty(),
        var cancelResult: NetworkResult<Unit> = NetworkResult.Empty(),
    ) : QrLoginApi {
        val scanned = mutableListOf<String>()
        val confirmed = mutableListOf<String>()
        val cancelled = mutableListOf<String>()

        override suspend fun scan(request: QrIdRequest): NetworkResult<QrScanResponse> {
            scanned.add(request.qrId)
            return scanResult
        }

        override suspend fun confirm(request: QrIdRequest): NetworkResult<Unit> {
            confirmed.add(request.qrId)
            return confirmResult
        }

        override suspend fun cancel(request: QrIdRequest): NetworkResult<Unit> {
            cancelled.add(request.qrId)
            return cancelResult
        }
    }

    private lateinit var api: FakeQrLoginApi
    private lateinit var viewModel: QrLoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        api = FakeQrLoginApi()
        viewModel = QrLoginViewModel(api, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `scan 成功进入待确认状态并携带网页端信息`() = runTest {
        api.scanResult = NetworkResult.Success(QrScanResponse(client = "网页端 Windows Chrome"))
        var confirmedCalled = false
        viewModel.confirm { confirmedCalled = true } // 空qrId应被忽略
        viewModel.scan("qr-1")
        advanceUntilIdle()

        val state = viewModel.qrState.value
        assertEquals("qr-1", state.qrId)
        assertEquals("网页端 Windows Chrome", state.client)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(confirmedCalled)
    }

    @Test
    fun `scan 失败展示错误并清空票据允许重扫`() = runTest {
        api.scanResult = NetworkResult.Failure(message = "二维码已过期，请刷新后重试")
        viewModel.scan("qr-1")
        advanceUntilIdle()

        val state = viewModel.qrState.value
        assertEquals("二维码已过期，请刷新后重试", state.error)
        assertEquals("", state.qrId)
        assertFalse(state.isLoading)
    }

    @Test
    fun `confirm 成功置位 isConfirmed 并回调`() = runTest {
        viewModel.scan("qr-1")
        advanceUntilIdle()
        var callback = ""
        viewModel.confirm { callback = "ok" }
        advanceUntilIdle()

        assertTrue(viewModel.qrState.value.isConfirmed)
        assertEquals("ok", callback)
        assertEquals(listOf("qr-1"), api.confirmed)
    }

    @Test
    fun `confirm 失败保留票据与错误提示可重试`() = runTest {
        viewModel.scan("qr-1")
        advanceUntilIdle()
        api.confirmResult = NetworkResult.Failure(message = "请先扫描网页端二维码")
        viewModel.confirm()
        advanceUntilIdle()

        val state = viewModel.qrState.value
        assertFalse(state.isConfirmed)
        assertEquals("请先扫描网页端二维码", state.error)
        assertEquals("qr-1", state.qrId)
    }

    @Test
    fun `cancel 成功回调并置位 isCancelled`() = runTest {
        viewModel.scan("qr-1")
        advanceUntilIdle()
        var callback = ""
        viewModel.cancel { callback = "done" }
        advanceUntilIdle()

        assertTrue(viewModel.qrState.value.isCancelled)
        assertEquals("done", callback)
        assertEquals(listOf("qr-1"), api.cancelled)
    }

    @Test
    fun `reset 清空全部状态`() = runTest {
        api.scanResult = NetworkResult.Success(QrScanResponse(client = "网页端"))
        viewModel.scan("qr-1")
        advanceUntilIdle()
        viewModel.reset()

        val state = viewModel.qrState.value
        assertEquals("", state.qrId)
        assertNull(state.client)
        assertNull(state.error)
        assertFalse(state.isConfirmed)
    }

    @Test
    fun `qrId 为空时 confirm 直接忽略`() = runTest {
        viewModel.confirm()
        advanceUntilIdle()
        assertTrue(api.confirmed.isEmpty())
    }
}
