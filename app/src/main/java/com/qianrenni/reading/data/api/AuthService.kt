package com.qianrenni.reading.data.api

import android.util.Log
import com.qianrenni.reading.data.model.EmailVerifyRequest
import com.qianrenni.reading.data.model.LoginRequest
import com.qianrenni.reading.data.model.LoginResponse
import com.qianrenni.reading.data.model.RegisterRequest
import com.qianrenni.reading.data.model.User
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.setBody

object AuthService {
    private var lastXCaptchaId: String? = ""

    suspend fun getCaptcha(): NetworkResult<ByteArray> {
        return try {
            val response = NetworkClient.client.get("${NetworkClient.getBaseUrl()}captcha/get")
            this.lastXCaptchaId = response.headers["X-Captcha-Id"]
            val image = response.body<ByteArray>()
            Log.d("captcha", "getCaptcha:${image.size} ")
            NetworkResult.Success(
                data = image,
            )
        } catch (e: Exception) {
            return NetworkResult.Failure(
                message = e.message ?: "Unknown Error",
                code = 500
            )
        }
    }

    suspend fun login(
        request: LoginRequest,
        captchaId: String = this.lastXCaptchaId ?: ""
    ): NetworkResult<LoginResponse> {
        return NetworkClient.post("token/get") {
            header("X-Captcha-Id", captchaId)
            setBody(request)
        }
    }

    suspend fun refreshToken(): NetworkResult<LoginResponse> {
        return NetworkClient.post("token/refresh")
    }

    suspend fun getCurrentUser(): NetworkResult<User> {
        return NetworkClient.get("token/auth/me")
    }

    suspend fun register(
        request: RegisterRequest,
    ): NetworkResult<Unit> {
        return NetworkClient.post("user/register") {
            header("X-Captcha-Id", this@AuthService.lastXCaptchaId)
            setBody(request)
        }
    }

    suspend fun verifyEmail(
        request: EmailVerifyRequest
    ): NetworkResult<Unit> {
        return NetworkClient.post("token/verify_email") {
            setBody(request)
        }
    }
}
