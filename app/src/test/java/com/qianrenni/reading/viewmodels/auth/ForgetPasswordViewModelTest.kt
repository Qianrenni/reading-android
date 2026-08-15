package com.qianrenni.reading.viewmodels.auth

import com.qianrenni.reading.FakeUserApi
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
class ForgetPasswordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fillValidForm(vm: ForgetPasswordViewModel) {
        vm.onEmailChange("t@e.c")
        vm.onCaptchaChange("cap")
        vm.onPasswordChange("pw")
        vm.onConfirmPasswordChange("pw")
    }

    @Test
    fun `sendVerificationCode success stops sending`() = runTest(testDispatcher) {
        val api = FakeUserApi().apply { sendCodeResult = NetworkResult.Empty() }
        val vm = ForgetPasswordViewModel(api, testDispatcher)
        vm.onEmailChange("t@e.c")

        vm.sendVerificationCode()
        advanceUntilIdle()

        assertFalse(vm.forgetPasswordState.value.isSendingCode)
    }

    @Test
    fun `sendVerificationCode with invalid email shows error`() = runTest(testDispatcher) {
        val vm = ForgetPasswordViewModel(FakeUserApi(), testDispatcher)
        vm.onEmailChange("bad")

        vm.sendVerificationCode()
        advanceUntilIdle()

        assertEquals("邮箱格式不正确", vm.forgetPasswordState.value.pageStatus.errorMessage)
    }

    @Test
    fun `resetPassword success clears loading`() = runTest(testDispatcher) {
        val api = FakeUserApi().apply { resetPasswordResult = NetworkResult.Empty() }
        val vm = ForgetPasswordViewModel(api, testDispatcher)
        fillValidForm(vm)

        vm.resetPassword()
        advanceUntilIdle()

        assertFalse(vm.forgetPasswordState.value.pageStatus.isLoading)
    }

    @Test
    fun `resetPassword with mismatched password shows error`() = runTest(testDispatcher) {
        val vm = ForgetPasswordViewModel(FakeUserApi(), testDispatcher)
        vm.onEmailChange("t@e.c")
        vm.onCaptchaChange("cap")
        vm.onPasswordChange("a")
        vm.onConfirmPasswordChange("b")

        vm.resetPassword()
        advanceUntilIdle()

        assertEquals("两次输入密码不一致", vm.forgetPasswordState.value.pageStatus.errorMessage)
    }

    @Test
    fun `resetPassword failure shows error`() = runTest(testDispatcher) {
        val api = FakeUserApi().apply { resetPasswordResult = NetworkResult.Failure("验证码错误", 1) }
        val vm = ForgetPasswordViewModel(api, testDispatcher)
        fillValidForm(vm)

        vm.resetPassword()
        advanceUntilIdle()

        assertTrue(vm.forgetPasswordState.value.pageStatus.isError)
        assertEquals("验证码错误", vm.forgetPasswordState.value.pageStatus.errorMessage)
    }
}
