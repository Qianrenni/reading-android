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

@Serializable
data class BookRead(val bookId: Int, val chapterId: Int) : NavKey

@Serializable
data class BookInfo(val bookId: Int) : NavKey
