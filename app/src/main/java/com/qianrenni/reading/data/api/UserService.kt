package com.qianrenni.reading.data.api

import com.qianrenni.reading.data.model.ForgotPasswordRequest
import com.qianrenni.reading.data.model.UpdatePasswordRequest
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody

object UserService {
    suspend fun updatePassword(
        request: UpdatePasswordRequest
    ): NetworkResult<Unit> {
        return NetworkClient.patch("user/update-password") {
            setBody(request)
        }
    }

    suspend fun sendForgotPasswordCode(
        userAccount: String
    ): NetworkResult<Unit> {
        return NetworkClient.get("user/forgot-password") {
            parameter("user_account", userAccount)
        }
    }

    suspend fun resetPassword(
        request: ForgotPasswordRequest
    ): NetworkResult<Unit> {
        return NetworkClient.patch("user/forgot-password") {
            setBody(request)
        }
    }
}