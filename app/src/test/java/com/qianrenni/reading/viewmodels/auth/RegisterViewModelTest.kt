package com.qianrenni.reading.viewmodels.auth

import com.qianrenni.reading.FakeAuthApi
import com.qianrenni.reading.data.remote.NetworkResult
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fillValidForm(vm: RegisterViewModel) {
        vm.onUsernameChange("tom")
        vm.onPasswordChange("pw")
        vm.onConfirmPasswordChange("pw")
        vm.onEmailChange("t@e.c")
        vm.onCaptchaChange("cap")
    }

    @Test
    fun `register success invokes onSuccess`() = runTest(testDispatcher) {
        val api = FakeAuthApi().apply { registerResult = NetworkResult.Empty() }
        val vm = RegisterViewModel(api, testDispatcher)
        fillValidForm(vm)

        var success = false
        vm.register(onSuccess = { success = true })
        advanceUntilIdle()

        assertTrue(success)
        assertFalse(vm.registerState.value.pageStatus.isLoading)
    }

    @Test
    fun `register with empty fields shows error`() = runTest(testDispatcher) {
        val vm = RegisterViewModel(FakeAuthApi(), testDispatcher)

        vm.register()
        advanceUntilIdle()

        assertEquals("请填写所有字段", vm.registerState.value.pageStatus.errorMessage)
    }

    @Test
    fun `register with mismatched password shows error`() = runTest(testDispatcher) {
        val vm = RegisterViewModel(FakeAuthApi(), testDispatcher)
        vm.onUsernameChange("tom")
        vm.onPasswordChange("a")
        vm.onConfirmPasswordChange("b")
        vm.onEmailChange("t@e.c")
        vm.onCaptchaChange("cap")

        vm.register()
        advanceUntilIdle()

        assertEquals("两次输入密码不一致", vm.registerState.value.pageStatus.errorMessage)
    }

    @Test
    fun `register with invalid email shows error`() = runTest(testDispatcher) {
        val vm = RegisterViewModel(FakeAuthApi(), testDispatcher)
        vm.onUsernameChange("tom")
        vm.onPasswordChange("pw")
        vm.onConfirmPasswordChange("pw")
        vm.onEmailChange("not-an-email")
        vm.onCaptchaChange("cap")

        vm.register()
        advanceUntilIdle()

        assertEquals("邮箱格式不正确", vm.registerState.value.pageStatus.errorMessage)
    }

    @Test
    fun `verifyEmail success invokes onSuccess`() = runTest(testDispatcher) {
        val api = FakeAuthApi().apply { verifyEmailResult = NetworkResult.Empty() }
        val vm = RegisterViewModel(api, testDispatcher)
        vm.onEmailChange("t@e.c")

        var ok = false
        vm.verifyEmail(onSuccess = { ok = true })
        advanceUntilIdle()

        assertTrue(ok)
        assertFalse(vm.registerState.value.isVerifyingEmail)
    }

    @Test
    fun `verifyEmail with invalid email shows error`() = runTest(testDispatcher) {
        val vm = RegisterViewModel(FakeAuthApi(), testDispatcher)
        vm.onEmailChange("bad")

        vm.verifyEmail()
        advanceUntilIdle()

        assertEquals("邮箱格式不正确", vm.registerState.value.pageStatus.errorMessage)
    }
}
