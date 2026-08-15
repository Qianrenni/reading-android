package com.qianrenni.reading.common

data class CommonPageStatus(
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null
) {
    // 可以在这里放一些简单的工厂方法，或者放在外面作为扩展函数
    fun loading() = copy(isLoading = true, isError = false, errorMessage = null)
    fun down() = copy(isLoading = false, isError = false, errorMessage = null)
    fun error(message: String) = copy(isLoading = false, isError = true, errorMessage = message)
}

interface CommonUiState {
    val pageStatus: CommonPageStatus
}