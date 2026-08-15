package com.qianrenni.reading.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.qianrenni.reading.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 持久化的令牌数据。
 */
data class SavedTokens(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
)

/**
 * 会话管理器：内存中的用户/令牌状态 + SharedPreferences 持久化。
 *
 * 替换原全局单例 AuthStore，通过 AppContainer 注入，可被测试替换为 Fake。
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var tokenType: String? = null

    /**
     * 构造 Authorization 头值（如 "Bearer xxx"），无令牌时返回 null。
     */
    fun authHeaderValue(): String? {
        val token = accessToken
        val type = tokenType
        return if (token != null && type != null) "$type $token" else null
    }

    fun setToken(
        accessToken: String,
        refreshToken: String,
        tokenType: String,
        isSave: Boolean = true
    ) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.tokenType = tokenType
        if (isSave) {
            saveTokens()
        }
    }

    fun setUser(user: User?) {
        _user.value = user
    }

    /**
     * 读取持久化的令牌；无则返回 null。
     */
    fun savedTokens(): SavedTokens? {
        val access = prefs.getString("access_token", null) ?: return null
        val refresh = prefs.getString("refresh_token", null) ?: return null
        val type = prefs.getString("token_type", null) ?: return null
        return SavedTokens(access, refresh, type)
    }

    private fun saveTokens() {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putString("token_type", tokenType)
            .apply()
    }

    /**
     * 清空内存状态与持久化数据。
     */
    fun clear() {
        setUser(null)
        accessToken = null
        refreshToken = null
        tokenType = null
        prefs.edit()
            .remove("access_token")
            .remove("refresh_token")
            .remove("token_type")
            .apply()
    }
}
