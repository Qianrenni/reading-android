package com.qianrenni.reading.util

/**
 * 邮箱格式校验（纯 Kotlin 实现，不依赖 android.util.Patterns，便于单元测试）。
 */
private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{1,}$")

fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email)
