package com.qianrenni.reading.viewmodels.qr

import com.qianrenni.reading.FakeAuthRepository
import com.qianrenni.reading.FakeQrLoginApi
import com.qianrenni.reading.data.model.QrLoginAction
import com.qianrenni.reading.data.model.QrLoginStatus
import com.qianrenni.reading.data.model.QrLoginStatusResponse
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.testUser
import com.qianrenni.reading.util.QrLoginPayload
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

/** QrScanViewModel 状态机测试：识别上报 → 用户确认 → 得到结果。 */
@OptIn(ExperimentalCoroutinesApi::class)
class QrScanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val ticket = "ticket-1"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        api: FakeQrLoginApi = FakeQrLoginApi(),
        user: com.qianrenni.reading.data.model.User? = testUser(name = "tom")
    ) = QrScanViewModel(api, FakeAuthRepository(user), testDispatcher)

    @Test
    fun `识别到登录码后上报 scan 并等待用户确认`() = runTest(testDispatcher) {
        val api = FakeQrLoginApi()
        val vm = viewModel(api)

        vm.onQrDetected(QrLoginPayload.build(ticket))
        advanceUntilIdle()

        assertEquals(listOf(ticket to QrLoginAction.SCAN), api.actions)
        assertEquals(ticket, vm.state.value.ticket)
        assertTrue(vm.state.value.isConfirming)
        assertNull(vm.state.value.message)
    }

    @Test
    fun `相机重复识别同一张码只上报一次`() = runTest(testDispatcher) {
        val api = FakeQrLoginApi()
        val vm = viewModel(api)
        val payload = QrLoginPayload.build(ticket)

        vm.onQrDetected(payload)
        advanceUntilIdle()
        repeat(5) { vm.onQrDetected(payload) }
        advanceUntilIdle()

        assertEquals(1, api.actions.size)
    }

    @Test
    fun `识别到非本系统二维码时只提示不上报`() = runTest(testDispatcher) {
        val api = FakeQrLoginApi()
        val vm = viewModel(api)

        vm.onQrDetected("https://example.com/whatever")
        advanceUntilIdle()

        assertTrue(api.actions.isEmpty())
        assertEquals("请扫描本系统网页端登录页的二维码", vm.state.value.message)
        assertFalse(vm.state.value.isConfirming)

        // 提示消费后可以继续扫描
        vm.clearMessage()
        assertNull(vm.state.value.message)
        vm.onQrDetected(QrLoginPayload.build(ticket))
        advanceUntilIdle()
        assertEquals(1, api.actions.size)
    }

    @Test
    fun `confirm 上报 confirm 并标记已确认`() = runTest(testDispatcher) {
        val api = FakeQrLoginApi()
        val vm = viewModel(api)
        vm.onQrDetected(QrLoginPayload.build(ticket))
        advanceUntilIdle()

        api.actionResult = NetworkResult.Success(
            QrLoginStatusResponse(QrLoginStatus.CONFIRMED)
        )
        vm.confirm()
        advanceUntilIdle()

        assertEquals(
            listOf(ticket to QrLoginAction.SCAN, ticket to QrLoginAction.CONFIRM),
            api.actions
        )
        assertTrue(vm.state.value.isConfirmed)
        assertFalse(vm.state.value.isConfirming)
        assertEquals("已确认，请回到网页端", vm.state.value.message)
    }

    @Test
    fun `cancel 上报 cancel 并回到取景状态`() = runTest(testDispatcher) {
        val api = FakeQrLoginApi()
        val vm = viewModel(api)
        vm.onQrDetected(QrLoginPayload.build(ticket))
        advanceUntilIdle()

        vm.cancel()
        advanceUntilIdle()

        assertEquals(
            listOf(ticket to QrLoginAction.SCAN, ticket to QrLoginAction.CANCEL),
            api.actions
        )
        assertEquals("", vm.state.value.ticket)
        assertFalse(vm.state.value.isConfirming)
        assertEquals("已取消登录", vm.state.value.message)
    }

    @Test
    fun `未识别到票据时 confirm 与 cancel 不产生请求`() = runTest(testDispatcher) {
        val api = FakeQrLoginApi()
        val vm = viewModel(api)

        vm.confirm()
        vm.cancel()
        advanceUntilIdle()

        assertTrue(api.actions.isEmpty())
    }

    @Test
    fun `上报失败时提示错误并允许重新扫描`() = runTest(testDispatcher) {
        val api = FakeQrLoginApi().apply {
            actionResult = NetworkResult.Failure("二维码已过期，请刷新后重试")
        }
        val vm = viewModel(api)

        vm.onQrDetected(QrLoginPayload.build(ticket))
        advanceUntilIdle()

        assertEquals("二维码已过期，请刷新后重试", vm.state.value.message)
        assertEquals("", vm.state.value.ticket)
        assertFalse(vm.state.value.isConfirming)

        api.actionResult = NetworkResult.Success(
            QrLoginStatusResponse(QrLoginStatus.SCANNED)
        )
        vm.clearMessage()
        vm.onQrDetected(QrLoginPayload.build(ticket))
        advanceUntilIdle()

        assertTrue(vm.state.value.isConfirming)
    }

    @Test
    fun `确认失败时回到取景状态`() = runTest(testDispatcher) {
        val api = FakeQrLoginApi()
        val vm = viewModel(api)
        vm.onQrDetected(QrLoginPayload.build(ticket))
        advanceUntilIdle()

        api.actionResult = NetworkResult.Failure("二维码已失效，请刷新后重试")
        vm.confirm()
        advanceUntilIdle()

        assertFalse(vm.state.value.isConfirmed)
        assertFalse(vm.state.value.isConfirming)
        assertEquals("二维码已失效，请刷新后重试", vm.state.value.message)
    }

    @Test
    fun `userName 暴露当前登录账号`() {
        assertEquals("tom", viewModel(user = testUser(name = "tom")).userName)
    }

    @Test
    fun `未登录时 userName 为空`() {
        assertNull(viewModel(user = null).userName)
    }
}
