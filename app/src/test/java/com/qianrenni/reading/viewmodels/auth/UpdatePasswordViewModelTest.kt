package com.qianrenni.reading.viewmodels.auth

import com.qianrenni.reading.FakeAuthRepository
import com.qianrenni.reading.FakeUserApi
import com.qianrenni.reading.data.remote.NetworkResult
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdatePasswordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updatePassword success clears session`() = runTest(testDispatcher) {
        val authRepo = FakeAuthRepository(testUser())
        val api = FakeUserApi().apply { updatePasswordResult = NetworkResult.Empty() }
        val vm = UpdatePasswordViewModel(api, authRepo, testDispatcher)
        vm.onEmailChange("t@e.c")
        vm.onOldPasswordChange("old")
        vm.onNewPasswordChange("new")
        vm.onConfirmPasswordChange("new")

        vm.updatePassword()
        advanceUntilIdle()

        assertNull(authRepo.userFlow.value)
        assertFalse(vm.updatePasswordState.value.pageStatus.isLoading)
    }

    @Test
    fun `updatePassword with empty fields shows error`() = runTest(testDispatcher) {
        val vm = UpdatePasswordViewModel(FakeUserApi(), FakeAuthRepository(), testDispatcher)

        vm.updatePassword()
        advanceUntilIdle()

        assertEquals("请填写所有字段", vm.updatePasswordState.value.pageStatus.errorMessage)
    }

    @Test
    fun `updatePassword with mismatched password shows error`() = runTest(testDispatcher) {
        val vm = UpdatePasswordViewModel(FakeUserApi(), FakeAuthRepository(), testDispatcher)
        vm.onEmailChange("t@e.c")
        vm.onOldPasswordChange("old")
        vm.onNewPasswordChange("a")
        vm.onConfirmPasswordChange("b")

        vm.updatePassword()
        advanceUntilIdle()

        assertEquals("两次输入密码不一致", vm.updatePasswordState.value.pageStatus.errorMessage)
    }

    @Test
    fun `updatePassword failure shows error and keeps session`() = runTest(testDispatcher) {
        val authRepo = FakeAuthRepository(testUser())
        val api = FakeUserApi().apply { updatePasswordResult = NetworkResult.Failure("旧密码错误", 1) }
        val vm = UpdatePasswordViewModel(api, authRepo, testDispatcher)
        vm.onEmailChange("t@e.c")
        vm.onOldPasswordChange("wrong")
        vm.onNewPasswordChange("new")
        vm.onConfirmPasswordChange("new")

        vm.updatePassword()
        advanceUntilIdle()

        assertTrue(vm.updatePasswordState.value.pageStatus.isError)
        assertEquals("旧密码错误", vm.updatePasswordState.value.pageStatus.errorMessage)
        assertEquals(1, authRepo.userFlow.value?.id)
    }
}
