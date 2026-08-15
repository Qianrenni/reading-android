package com.qianrenni.reading.data.repository

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
 * 会话管理器：内存中的用户/令牌状态 + [KeyValueStore] 持久化。
 *
 * 存储实现由外部注入（生产环境为 [EncryptedKeyValueStore]），
 * 便于单元测试替换为内存实现。
 */
class SessionManager(private val store: KeyValueStore) {

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
        val access = store.getString(KEY_ACCESS) ?: return null
        val refresh = store.getString(KEY_REFRESH) ?: return null
        val type = store.getString(KEY_TYPE) ?: return null
        return SavedTokens(access, refresh, type)
    }

    private fun saveTokens() {
        store.putString(KEY_ACCESS, accessToken.orEmpty())
        store.putString(KEY_REFRESH, refreshToken.orEmpty())
        store.putString(KEY_TYPE, tokenType.orEmpty())
    }

    /**
     * 清空内存状态与持久化数据。
     */
    fun clear() {
        setUser(null)
        accessToken = null
        refreshToken = null
        tokenType = null
        store.remove(KEY_ACCESS)
        store.remove(KEY_REFRESH)
        store.remove(KEY_TYPE)
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_TYPE = "token_type"
    }
}
