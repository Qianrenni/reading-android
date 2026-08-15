package com.qianrenni.reading.viewmodels.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.qianrenni.reading.data.store.AuthStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


class AuthViewModel : ViewModel() {

    //1. 响应式状态：直接从 AuthStore 映射过来
    // stateIn 将 Flow 转换为 Hot Flow，并在 ViewModel 生命周期内共享
    val isLogin = AuthStore.user.map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // 立即开始监听
            initialValue = false
        )
    var redirectNavKey: NavKey? = null
    fun setRedirectUrl(url: NavKey?) {
        redirectNavKey = url
    }

    fun getRedirectUrl(): NavKey? {
        val res = redirectNavKey
        redirectNavKey = null
        return res
    }

    fun getUser() = AuthStore.user
    fun clear() {
        AuthStore.clear()
    }
}