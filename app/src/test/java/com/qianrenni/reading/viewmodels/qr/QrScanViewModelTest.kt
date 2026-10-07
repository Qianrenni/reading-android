package com.qianrenni.reading.viewmodels.qr

import com.qianrenni.reading.FakeAuthRepository
import com.qianrenni.reading.FakeBundleApi
import com.qianrenni.reading.FakeHybridBundleRepository
import com.qianrenni.reading.FakeQrLoginApi
import com.qianrenni.reading.data.model.QrLoginAction
import com.qianrenni.reading.data.model.QrLoginStatus
import com.qianrenni.reading.data.model.QrLoginStatusResponse
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.data.repository.BundleInstallResult
import com.qianrenni.reading.testBundleInfo
import com.qianrenni.reading.testInstalledBundle
import com.qianrenni.reading.testUser
import com.qianrenni.reading.util.BundlePayload
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
    private val md5 = "0123456789abcdef0123456789abcdef"

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
        user: com.qianrenni.reading.data.model.User? = testUser(name = "tom"),
        bundleApi: FakeBundleApi = FakeBundleApi(),
        bundles: FakeHybridBundleRepository = FakeHybridBundleRepository(),
    ) = QrScanViewModel(api, FakeAuthRepository(user), bundleApi, bundles, testDispatcher)

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
        assertEquals("请扫描本系统的登录码或页面包二维码", vm.state.value.message)
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

    // ---- 页面包扫码（扫码预发）----

    @Test
    fun `扫到页面包码时先等用户确认再请求服务端`() = runTest(testDispatcher) {
        val bundleApi = FakeBundleApi()
        val bundles = FakeHybridBundleRepository()
        val vm = viewModel(bundleApi = bundleApi, bundles = bundles)

        vm.onQrDetected(BundlePayload.build("demo", 7, md5))
        advanceUntilIdle()

        assertEquals("demo", vm.state.value.pendingBundle?.appKey)
        assertEquals(7, vm.state.value.pendingBundle?.versionCode)
        assertFalse(vm.state.value.isConfirming)
        assertTrue("确认前不应请求服务端", bundleApi.infoCalls.isEmpty())
        assertTrue(bundles.installCalls.isEmpty())
    }

    @Test
    fun `确认安装后暴露可渲染的路由`() = runTest(testDispatcher) {
        val bundleApi = FakeBundleApi().apply {
            infoResult = NetworkResult.Success(testBundleInfo(route = "hybrid-demo-rn", versionCode = 7))
        }
        val bundles = FakeHybridBundleRepository().apply {
            installResult = BundleInstallResult.Installed(
                testInstalledBundle(route = "hybrid-demo-rn", engine = "rn", versionCode = 7)
            )
        }
        val vm = viewModel(bundleApi = bundleApi, bundles = bundles)

        vm.onQrDetected(BundlePayload.build("demo", 7, md5))
        advanceUntilIdle()
        vm.confirmBundleInstall()
        advanceUntilIdle()

        assertEquals(listOf("demo" to 7), bundleApi.infoCalls)
        assertEquals(1, bundles.installCalls.size)
        assertEquals("hybrid-demo-rn", vm.state.value.installedRoute)
        assertNull(vm.state.value.pendingBundle)
        assertTrue(vm.state.value.message!!.contains("已就绪"))

        // 路由只消费一次，避免返回本页时重复跳转
        assertEquals("hybrid-demo-rn", vm.consumeInstalledRoute())
        assertNull(vm.consumeInstalledRoute())
    }

    @Test
    fun `包信息拉取失败时提示且不下载`() = runTest(testDispatcher) {
        val bundleApi = FakeBundleApi().apply { infoResult = NetworkResult.Failure("版本不存在") }
        val bundles = FakeHybridBundleRepository()
        val vm = viewModel(bundleApi = bundleApi, bundles = bundles)

        vm.onQrDetected(BundlePayload.build("demo", 9, md5))
        advanceUntilIdle()
        vm.confirmBundleInstall()
        advanceUntilIdle()

        assertTrue(bundles.installCalls.isEmpty())
        assertNull(vm.state.value.installedRoute)
        assertEquals("版本不存在", vm.state.value.message)
    }

    @Test
    fun `安装失败时提示原因且不产生路由`() = runTest(testDispatcher) {
        val bundleApi = FakeBundleApi().apply {
            infoResult = NetworkResult.Success(testBundleInfo(versionCode = 2))
        }
        val bundles = FakeHybridBundleRepository().apply {
            installResult = BundleInstallResult.Failed("包校验失败，请重新扫码下载")
        }
        val vm = viewModel(bundleApi = bundleApi, bundles = bundles)

        vm.onQrDetected(BundlePayload.build("demo", 2, md5))
        advanceUntilIdle()
        vm.confirmBundleInstall()
        advanceUntilIdle()

        assertNull(vm.state.value.installedRoute)
        assertEquals("包校验失败，请重新扫码下载", vm.state.value.message)
    }

    @Test
    fun `取消安装页面包后可以继续扫码`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.onQrDetected(BundlePayload.build("demo", 1, md5))
        advanceUntilIdle()
        vm.cancelBundle()
        assertNull(vm.state.value.pendingBundle)

        vm.onQrDetected(QrLoginPayload.build(ticket))
        advanceUntilIdle()
        assertTrue(vm.state.value.isConfirming)
    }

    @Test
    fun `等待页面包确认时不会误处理登录码`() = runTest(testDispatcher) {
        val api = FakeQrLoginApi()
        val vm = viewModel(api = api)

        vm.onQrDetected(BundlePayload.build("demo", 1, md5))
        advanceUntilIdle()
        vm.onQrDetected(QrLoginPayload.build(ticket))
        advanceUntilIdle()

        assertTrue(api.actions.isEmpty())
        assertTrue(vm.state.value.isConfirming.not())
        assertEquals(1, vm.state.value.pendingBundle?.versionCode)
    }
}
