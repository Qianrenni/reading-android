package com.qianrenni.reading

import com.qianrenni.reading.data.model.EmailVerifyRequest
import com.qianrenni.reading.data.model.LoginRequest
import com.qianrenni.reading.data.model.LoginResponse
import com.qianrenni.reading.data.model.RegisterRequest
import com.qianrenni.reading.data.model.User
import com.qianrenni.reading.data.remote.AuthApi
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * androidTest（仪器化测试）专用测试替身。
 * 与 test（JVM 单测）源码集相互隔离，因此单独定义。
 */

class FakeAuthApi : AuthApi {
    var loginResult: NetworkResult<LoginResponse> = NetworkResult.Failure("n/a")
    var loginCalled = false
    var lastLoginRequest: LoginRequest? = null

    override suspend fun getCaptcha() = NetworkResult.Failure("n/a")
    override suspend fun login(request: LoginRequest, captchaId: String?): NetworkResult<LoginResponse> {
        loginCalled = true
        lastLoginRequest = request
        return loginResult
    }

    override suspend fun getCurrentUser() = NetworkResult.Failure("n/a")
    override suspend fun register(request: RegisterRequest) = NetworkResult.Empty()
    override suspend fun verifyEmail(request: EmailVerifyRequest) = NetworkResult.Empty()
}

class FakeAuthRepository : AuthRepository {
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

fun testUser(id: Int = 1, name: String = "tom") =
    User(id = id, userName = name, email = "$name@e.c", isActive = true)
