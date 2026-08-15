package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.ForgotPasswordRequest
import com.qianrenni.reading.data.model.UpdatePasswordRequest
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody

/**
 * 用户相关 API。
 */
interface UserApi {
    suspend fun updatePassword(request: UpdatePasswordRequest): NetworkResult<Unit>
    suspend fun sendForgotPasswordCode(userAccount: String): NetworkResult<Unit>
    suspend fun resetPassword(request: ForgotPasswordRequest): NetworkResult<Unit>
}

class UserApiImpl(private val apiClient: ApiClient) : UserApi {

    override suspend fun updatePassword(request: UpdatePasswordRequest): NetworkResult<Unit> {
        return apiClient.patch("user/update-password") {
            setBody(request)
        }
    }

    override suspend fun sendForgotPasswordCode(userAccount: String): NetworkResult<Unit> {
        return apiClient.get("user/forgot-password") {
            parameter("user_account", userAccount)
        }
    }

    override suspend fun resetPassword(request: ForgotPasswordRequest): NetworkResult<Unit> {
        return apiClient.patch("user/forgot-password") {
            setBody(request)
        }
    }
}
