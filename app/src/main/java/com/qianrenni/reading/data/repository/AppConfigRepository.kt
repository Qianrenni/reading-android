package com.qianrenni.reading.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 默认后端地址（首次启动或未配置时使用）。 */
const val DEFAULT_BASE_URL = "http://49.235.107.221:8000/"

/**
 * 应用级配置仓库：服务器地址等，持久化到 [KeyValueStore]，
 * 打包后仍可在应用内动态修改、立即生效（无需重启）。
 */
interface AppConfigRepository {
    val baseUrl: StateFlow<String>

    /** 同步读取当前 base url（供网络层每次请求读取）。 */
    fun currentBaseUrl(): String

    /** 修改服务器地址并持久化，立即生效。 */
    fun setBaseUrl(url: String)
}

class AppConfigRepositoryImpl(private val store: KeyValueStore) : AppConfigRepository {

    private val _baseUrl = MutableStateFlow(
        store.getString(KEY_BASE_URL) ?: DEFAULT_BASE_URL
    )

    override val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    override fun currentBaseUrl(): String = _baseUrl.value

    override fun setBaseUrl(url: String) {
        val normalized = url.trim().trimEnd('/') + "/"
        store.putString(KEY_BASE_URL, normalized)
        _baseUrl.value = normalized
    }

    private companion object {
        const val KEY_BASE_URL = "base_url"
    }
}
