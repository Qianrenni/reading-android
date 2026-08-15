package com.qianrenni.reading.viewmodels.auth

import com.qianrenni.reading.data.model.EmailVerifyRequest
import com.qianrenni.reading.data.model.LoginRequest
import com.qianrenni.reading.data.model.LoginResponse
import com.qianrenni.reading.data.model.RegisterRequest
import com.qianrenni.reading.data.model.User
import com.qianrenni.reading.data.remote.AuthApi
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeAuthApi(var loginResult: NetworkResult<LoginResponse>) : AuthApi {
        override suspend fun getCaptcha() = NetworkResult.Failure("n/a")
        override suspend fun login(request: LoginRequest, captchaId: String?) = loginResult
        override suspend fun refreshToken() = NetworkResult.Failure("n/a")
        override suspend fun getCurrentUser() = NetworkResult.Failure("n/a")
        override suspend fun register(request: RegisterRequest) = NetworkResult.Empty()
        override suspend fun verifyEmail(request: EmailVerifyRequest) = NetworkResult.Empty()
    }

    private class FakeAuthRepository : AuthRepository {
        val userFlow = MutableStateFlow<User?>(null)
        override val user: StateFlow<User?> = userFlow
        override suspend fun initial() = Unit
        override fun setToken(accessToken: String, refreshToken: String, tokenType: String, isSave: Boolean) = Unit
        override fun setUser(user: User?) {
            userFlow.value = user
        }

        override fun clear() {
            userFlow.value = null
        }
    }

    private lateinit var repository: FakeAuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeAuthRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun loginResponse() = LoginResponse(
        accessToken = "access",
        refreshToken = "refresh",
        tokenType = "Bearer",
        user = User(id = 1, userName = "tom", email = "t@e.c", isActive = true)
    )

    @Test
    fun `login success stores user and token`() = runTest(testDispatcher) {
        val api = FakeAuthApi(NetworkResult.Success(loginResponse()))
        val vm = LoginViewModel(api, repository, testDispatcher)

        vm.onUsernameChange("tom")
        vm.onPasswordChange("secret")
        vm.onCaptchaChange("abcd")
        vm.login()

        advanceUntilIdle()

        assertNotNull(repository.userFlow.value)
        assertEquals(1, repository.userFlow.value?.id)
        assertFalse(vm.loginState.value.isLoading)
        assertNull(vm.loginState.value.error)
    }

    @Test
    fun `login failure keeps user null and sets error`() = runTest(testDispatcher) {
        val api = FakeAuthApi(NetworkResult.Failure("用户名或密码错误", code = 1))
        val vm = LoginViewModel(api, repository, testDispatcher)

        vm.onUsernameChange("tom")
        vm.onPasswordChange("wrong")
        vm.onCaptchaChange("abcd")
        vm.login()

        advanceUntilIdle()

        assertNull(repository.userFlow.value)
        assertFalse(vm.loginState.value.isLoading)
        assertEquals("用户名或密码错误", vm.loginState.value.error)
    }

    @Test
    fun `login with empty fields fails without calling api`() = runTest(testDispatcher) {
        var apiCalled = false
        val api = object : AuthApi {
            override suspend fun getCaptcha() = NetworkResult.Failure("n/a")
            override suspend fun login(request: LoginRequest, captchaId: String?): NetworkResult<LoginResponse> {
                apiCalled = true
                return NetworkResult.Success(loginResponse())
            }

            override suspend fun refreshToken() = NetworkResult.Failure("n/a")
            override suspend fun getCurrentUser() = NetworkResult.Failure("n/a")
            override suspend fun register(request: RegisterRequest) = NetworkResult.Empty()
            override suspend fun verifyEmail(request: EmailVerifyRequest) = NetworkResult.Empty()
        }
        val vm = LoginViewModel(api, repository, testDispatcher)

        vm.login()
        advanceUntilIdle()

        assertFalse(apiCalled)
        assertNull(repository.userFlow.value)
        assertEquals("输入格式错误", vm.loginState.value.error)
    }
}
