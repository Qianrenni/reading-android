package com.qianrenni.reading.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenRequest(
    val deviceToken: String
)

/** 服务端签发的设备凭据（指纹解锁登录用），本地需放进加密存储。 */
@Serializable
data class DeviceCredential(
    val deviceToken: String,
    val expiresIn: Long
)

/**
 * 用设备凭据换到的结果。
 *
 * [deviceToken] 是**轮换后**的新凭据（旧凭据已作废），必须覆盖保存。
 */
@Serializable
data class DeviceLoginResponse(
    val deviceToken: String,
    val expiresIn: Long,
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val user: User
) {
    /** 转成与密码登录一致的响应，便于复用同一套登录落地逻辑。 */
    fun toLoginResponse(): LoginResponse =
        LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = tokenType,
            user = user
        )
}
