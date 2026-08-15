package com.qianrenni.reading.data.repository

import com.qianrenni.reading.data.model.User
import com.qianrenni.reading.data.remote.AuthApi
import com.qianrenni.reading.data.remote.NetworkResult
import kotlinx.coroutines.flow.StateFlow

/**
 * 认证仓库：面向 UI 层暴露登录状态与令牌管理。
 */
interface AuthRepository {
    val user: StateFlow<User?>

    /** 恢复持久化的会话：先尝试旧 token 获取用户，失败则用 refresh token 刷新 */
    suspend fun initial()

    fun setToken(accessToken: String, refreshToken: String, tokenType: String, isSave: Boolean = true)
    fun setUser(user: User?)
    fun clear()
}

class AuthRepositoryImpl(
    private val session: SessionManager,
    private val authApi: AuthApi,
) : AuthRepository {

    override val user: StateFlow<User?>
        get() = session.user

    override suspend fun initial() {
        val saved = session.savedTokens() ?: return

        // 尝试 1：用旧 access token 获取用户
        session.setToken(saved.accessToken, saved.refreshToken, saved.tokenType, isSave = false)
        val currentUser = authApi.getCurrentUser()
        if (currentUser is NetworkResult.Success) {
            session.setUser(currentUser.data)
            return
        }

        // 尝试 2：用 refresh token 刷新令牌
        session.setToken(saved.refreshToken, saved.refreshToken, saved.tokenType, isSave = false)
        val refreshed = authApi.refreshToken()
        if (refreshed is NetworkResult.Success) {
            session.setToken(refreshed.data.accessToken, refreshed.data.refreshToken, refreshed.data.tokenType)
            session.setUser(refreshed.data.user)
        }
    }

    override fun setToken(accessToken: String, refreshToken: String, tokenType: String, isSave: Boolean) {
        session.setToken(accessToken, refreshToken, tokenType, isSave)
    }

    override fun setUser(user: User?) {
        session.setUser(user)
    }

    override fun clear() {
        session.clear()
    }
}
