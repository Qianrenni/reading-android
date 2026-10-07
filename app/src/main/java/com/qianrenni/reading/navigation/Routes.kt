package com.qianrenni.reading.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 所有导航路由（NavKey）集中在 navigation 包内。 */
@Serializable
data object Home : NavKey

@Serializable
data object Login : NavKey

@Serializable
data object Register : NavKey

@Serializable
data object ForgetPassword : NavKey

@Serializable
data object UpdatePassword : NavKey

@Serializable
data object Bookshelf : NavKey

@Serializable
data object History : NavKey

@Serializable
data object Profile : NavKey

@Serializable
data object PrivacyPolicy : NavKey

/** 扫码登录网页端（相机扫码 + 手机端确认）。 */
@Serializable
data object QrScan : NavKey

/**
 * 容器页面路由：同一条 [route] 的 native / H5 / RN 实现由容器路由表解析决定，
 * 调用方（含 H5/RN 页面内部跳转）不需要关心最终由谁渲染。
 *
 * @param engine 强制指定引擎（native/h5/rn），用于扫码预发与调试；null 表示交给容器决策
 */
@Serializable
data class HybridPage(
    val route: String,
    val title: String? = null,
    val engine: String? = null
) : NavKey

/** 通用 H5（WebView）页面；[title] 为空时显示网页自身标题。 */
@Serializable
data class WebPage(val url: String, val title: String? = null) : NavKey

@Serializable
data class BookRead(val bookId: Int, val chapterId: Int) : NavKey

@Serializable
data class BookInfo(val bookId: Int) : NavKey
