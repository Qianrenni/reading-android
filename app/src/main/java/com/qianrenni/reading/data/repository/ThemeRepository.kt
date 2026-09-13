package com.qianrenni.reading.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 应用外观主题：全应用统一（阅读页同样跟随），预设 5 套主题 + 1 个跟随系统模式。
 *
 * - [LIGHT] 白天
 * - [DARK] 黑夜
 * - [GREEN] 绿色护眼
 * - [PARCHMENT] 羊皮纸（纸张质感）
 * - [WOOD] 木制家具（木纹质感）
 * - [SYSTEM] 跟随系统：按系统深浅色在 [LIGHT] / [DARK] 之间切换
 *
 * [displayName] 用于界面展示，[name] 作为持久化存储值。
 */
enum class ThemeMode(
    val displayName: String,
    private val darkPalette: Boolean = false,
    val followsSystem: Boolean = false
) {
    SYSTEM("跟随系统", followsSystem = true),
    LIGHT("白天"),
    DARK("黑夜", darkPalette = true),
    GREEN("绿色护眼"),
    PARCHMENT("羊皮纸"),
    WOOD("木制家具", darkPalette = true);

    /**
     * 当前主题是否为深色：[SYSTEM] 跟随系统深浅色，其余主题由自身配色决定。
     * 用于 Material 配色选择与系统状态栏图标颜色。
     */
    fun isDark(systemDark: Boolean): Boolean = if (followsSystem) systemDark else darkPalette

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
