package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.EmailVerifyRequest
import com.qianrenni.reading.data.model.LoginRequest
import com.qianrenni.reading.data.model.LoginResponse
import com.qianrenni.reading.data.model.RegisterRequest
import com.qianrenni.reading.data.model.User
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.setBody

/**
 * 认证相关 API。
 */
interface AuthApi {
    suspend fun getCaptcha(): NetworkResult<ByteArray>
    suspend fun login(request: LoginRequest, captchaId: String?): NetworkResult<LoginResponse>
    suspend fun refreshToken(): NetworkResult<LoginResponse>
    suspend fun getCurrentUser(): NetworkResult<User>
    suspend fun register(request: RegisterRequest): NetworkResult<Unit>
    suspend fun verifyEmail(request: EmailVerifyRequest): NetworkResult<Unit>
}

class AuthApiImpl(private val apiClient: ApiClient) : AuthApi {

    private var lastXCaptchaId: String? = null

    override suspend fun getCaptcha(): NetworkResult<ByteArray> {
        return try {
            val response = apiClient.client.get("${apiClient.baseUrl}captcha/get")
            lastXCaptchaId = response.headers["X-Captcha-Id"]
            val image = response.body<ByteArray>()
            NetworkResult.Success(data = image)
        } catch (e: Exception) {
            NetworkResult.Failure(
                message = e.message ?: "Unknown Error",
                code = 500
            )
        }
    }

    override suspend fun login(
        request: LoginRequest,
        captchaId: String?
    ): NetworkResult<LoginResponse> {
        return apiClient.post("token/get") {
            header("X-Captcha-Id", captchaId ?: lastXCaptchaId ?: "")
            setBody(request)
        }
    }

    override suspend fun refreshToken(): NetworkResult<LoginResponse> {
        return apiClient.post("token/refresh")
    }

    override suspend fun getCurrentUser(): NetworkResult<User> {
        return apiClient.get("token/auth/me")
    }

    override suspend fun register(
        request: RegisterRequest,
    ): NetworkResult<Unit> {
        return apiClient.post("user/register") {
            header("X-Captcha-Id", lastXCaptchaId ?: "")
            setBody(request)
        }
    }

    override suspend fun verifyEmail(
        request: EmailVerifyRequest
    ): NetworkResult<Unit> {
        return apiClient.post("token/verify_email") {
            setBody(request)
        }
    }
}
