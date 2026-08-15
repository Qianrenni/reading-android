package com.qianrenni.reading.util

import android.app.Activity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 系统栏控制工具类
 * 用于控制状态栏和导航栏的显示/隐藏
 */
object SystemBarUtils {

    /**
     * 隐藏系统状态栏和导航栏（沉浸式模式）
     */
    fun hideSystemBars(activity: Activity) {
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    /**
     * 显示系统状态栏和导航栏
     */
    fun showSystemBars(activity: Activity) {
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.navigationBars())
        controller.show(WindowInsetsCompat.Type.statusBars())

    }
}
