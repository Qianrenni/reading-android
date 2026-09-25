package com.qianrenni.reading.viewmodels.auth

import com.qianrenni.reading.FakeAuthRepository
import com.qianrenni.reading.FakeDeviceLoginApi
import com.qianrenni.reading.InMemoryKeyValueStore
import com.qianrenni.reading.data.model.DeviceCredential
import com.qianrenni.reading.data.model.DeviceLoginResponse
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.data.repository.BiometricLoginRepositoryImpl
import com.qianrenni.reading.testUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 指纹解锁 ViewModel 测试：设备能力闸门、指纹框请求/回报、凭据换取会话。 */
@OptIn(ExperimentalCoroutinesApi::class)
class BiometricViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val store = InMemoryKeyValueStore()
    private val api = FakeDeviceLoginApi()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        authRepository: FakeAuthRepository = FakeAuthRepository(),
        available: Boolean = true
    ) = BiometricViewModel(
        biometricRepository = BiometricLoginRepositoryImpl(store, api),
        authRepository = authRepository,
        isBiometricAvailable = { available },
        ioDispatcher = testDispatcher
    )

    @Test
    fun `设备不支持时只提示 不请求指纹框`() = runTest(testDispatcher) {
        val vm = viewModel(available = false)

        vm.requestLogin()
        advanceUntilIdle()

        assertNull(vm.state.value.pendingAction)
        assertTrue(vm.state.value.message!!.contains("未录入"))
    }

    @Test
    fun `requestLogin 进入等待指纹框状态`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.requestLogin()

        assertEquals(BiometricAction.LOGIN, vm.state.value.pendingAction)
        assertNull(vm.state.value.message)
    }

    @Test
    fun `requestEnable 进入等待指纹框状态`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.requestEnable()

        assertEquals(BiometricAction.ENABLE, vm.state.value.pendingAction)
    }

    @Test
    fun `指纹框取消后清除等待状态并提示`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.requestLogin()

        vm.onPromptFailed()

        assertNull(vm.state.value.pendingAction)
        assertEquals("指纹验证未通过", vm.state.value.message)
    }

    @Test
    fun `未请求时回报指纹结果不产生副作用`() = runTest(testDispatcher) {
        val authRepository = FakeAuthRepository()
        val vm = viewModel(authRepository)

        vm.onPromptSucceeded()
        vm.onPromptFailed()
        advanceUntilIdle()

        assertNull(vm.state.value.pendingAction)
        assertNull(authRepository.user.value)
    }

    @Test
    fun `指纹通过后用凭据登录并写入会话`() = runTest(testDispatcher) {
        store.putString("device_token", "device-1")
        api.loginResult = NetworkResult.Success(
            DeviceLoginResponse(
                deviceToken = "device-2",
                expiresIn = 1000,
                accessToken = "access",
                refreshToken = "refresh",
                tokenType = "Bearer",
                user = testUser(name = "tom")
            )
        )
        val authRepository = FakeAuthRepository()
        val vm = viewModel(authRepository)

        vm.requestLogin()
        vm.onPromptSucceeded()
        advanceUntilIdle()

        assertEquals("tom", authRepository.user.value?.userName)
        assertEquals(false, vm.state.value.isBusy)
        // 凭据已轮换保存
        assertEquals("device-2", store.getString("device_token"))
    }

    @Test
    fun `凭据失效时提示改用密码登录`() = runTest(testDispatcher) {
        store.putString("device_token", "device-1")
        api.loginResult = NetworkResult.Failure("指纹登录已失效", code = 1)
        val authRepository = FakeAuthRepository()
        val vm = viewModel(authRepository)

        vm.requestLogin()
        vm.onPromptSucceeded()
        advanceUntilIdle()

        assertNull(authRepository.user.value)
        assertTrue(vm.state.value.message!!.contains("账号密码"))
        // 失效凭据已被清除，指纹入口随之隐藏
        assertEquals(false, vm.isEnabled.value)
    }

    @Test
    fun `指纹通过后开启指纹解锁`() = runTest(testDispatcher) {
        api.createResult = NetworkResult.Success(DeviceCredential("device-9", 1000))
        val vm = viewModel()

        vm.requestEnable()
        vm.onPromptSucceeded()
        advanceUntilIdle()

        assertTrue(vm.isEnabled.value)
        assertEquals("device-9", store.getString("device_token"))
        assertTrue(vm.state.value.message!!.contains("已开启"))
    }

    @Test
    fun `开启失败时给出提示且保持未开启`() = runTest(testDispatcher) {
        api.createResult = NetworkResult.Failure("服务不可用")
        val vm = viewModel()

        vm.requestEnable()
        vm.onPromptSucceeded()
        advanceUntilIdle()

        assertEquals(false, vm.isEnabled.value)
        assertTrue(vm.state.value.message!!.contains("开启失败"))
    }

    @Test
    fun `disable 关闭指纹解锁`() = runTest(testDispatcher) {
        store.putString("device_token", "device-1")
        val vm = viewModel()

        vm.disable()
        advanceUntilIdle()

        assertEquals(false, vm.isEnabled.value)
        assertEquals(1, api.revokeCount)
        assertEquals("已关闭指纹解锁", vm.state.value.message)
    }

    @Test
    fun `clearMessage 清空提示`() = runTest(testDispatcher) {
        val vm = viewModel(available = false)
        vm.requestLogin()

        vm.clearMessage()

        assertNull(vm.state.value.message)
    }

    @Test
    fun `isAvailable 透传设备能力`() {
        assertTrue(viewModel(available = true).isAvailable())
        assertEquals(false, viewModel(available = false).isAvailable())
    }

    @Test
    fun `isEnabled 反映仓库状态`() {
        store.putString("device_token", "device-1")
        val vm = viewModel()

        assertTrue(vm.isEnabled.value)
    }

    @Test
    fun `指纹框文案按动作区分`() {
        assertEquals("指纹解锁登录", biometricPromptText(BiometricAction.LOGIN).title)
        assertEquals("开启指纹解锁", biometricPromptText(BiometricAction.ENABLE).title)
    }
}
