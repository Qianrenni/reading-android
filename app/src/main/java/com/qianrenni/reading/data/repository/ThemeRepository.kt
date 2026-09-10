package com.qianrenni.reading.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 应用外观模式：跟随系统 / 白天 / 黑夜。
 *
 * [displayName] 用于界面展示，[name] 作为持久化存储值。
 */
enum class ThemeMode(val displayName: String) {
    SYSTEM("跟随系统"),
    LIGHT("白天"),
    DARK("黑夜");

    companion object {
        /** 解析持久化的存储值，为空或无法识别时回退到 [SYSTEM]。 */
        fun fromStorage(value: String?): ThemeMode =
            entries.find { it.name == value } ?: SYSTEM
    }
}

/**
 * 主题模式仓库：持久化到 [KeyValueStore]，通过 [mode] 暴露给界面即时响应切换。
 */
interface ThemeRepository {
    val mode: StateFlow<ThemeMode>

    /** 切换主题模式并持久化，立即生效。 */
    fun setMode(mode: ThemeMode)
}

class ThemeRepositoryImpl(private val store: KeyValueStore) : ThemeRepository {

    private val _mode = MutableStateFlow(ThemeMode.fromStorage(store.getString(KEY_THEME_MODE)))

    override val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    override fun setMode(mode: ThemeMode) {
        store.putString(KEY_THEME_MODE, mode.name)
        _mode.value = mode
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
    }
}
