package com.qianrenni.reading.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.qianrenni.reading.data.model.User
import io.ktor.client.plugins.auth.providers.BearerTokens
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
 * 会话管理器：内存中的用户/令牌状态 + EncryptedSharedPreferences 加密持久化。
 *
 * 替换原全局单例 AuthStore，通过 AppContainer 注入，可被测试替换为 Fake。
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var tokenType: String? = null

    /**
     * 供 Ktor Auth 插件读取当前令牌；无则返回 null（不携带 Authorization 头）。
     */
    fun bearerTokens(): BearerTokens? {
        val access = accessToken ?: return null
        val refresh = refreshToken ?: return null
        return BearerTokens(access, refresh)
    }

    /**
     * 读取当前内存中的令牌（含 tokenType）。
     */
    fun tokens(): SavedTokens? {
        val access = accessToken ?: return null
        val refresh = refreshToken ?: return null
        val type = tokenType ?: return null
        return SavedTokens(access, refresh, type)
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
     * 读取持久化的令牌（应用启动时恢复会话用）；无则返回 null。
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

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
