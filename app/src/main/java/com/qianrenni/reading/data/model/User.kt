package com.qianrenni.reading.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val userName: String,
    val email: String,
    val avatar: String = "",
    val isActive: Boolean,
    val right: List<Int> = emptyList()
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val captcha: String
)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val user: User
)

@Serializable
data class UserRegister(
    val userName: String,
    val password: String,
    val email: String,
    val avatar: String = ""
)

@Serializable
data class RegisterRequest(
    val user: UserRegister,
    val captcha: String
)

@Serializable
data class EmailVerifyRequest(
    val email: String
)

@Serializable
data class UpdatePasswordRequest(
    val userName: String,
    val oldPassword: String,
    val newPassword: String
)

@Serializable
data class ForgotPasswordRequest(
    val userAccount: String,
    val verifyCode: String,
    val password: String
)