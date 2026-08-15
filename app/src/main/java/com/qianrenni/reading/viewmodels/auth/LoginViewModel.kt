package com.qianrenni.reading.viewmodels.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qianrenni.reading.data.api.AuthService
import com.qianrenni.reading.data.model.LoginRequest
import com.qianrenni.reading.data.store.AuthStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val username: String = "",
    val password: String = "",
    val captcha: String = "",
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

class LoginViewModel : ViewModel() {
    private val _loginState = MutableStateFlow(LoginState())
    val loginState = _loginState.asStateFlow()
    fun start() {
        viewModelScope.launch(Dispatchers.IO) {
            AuthStore.initial()
        }
    }

    // 更新输入字段 (辅助函数，简化 UI 调用)
    fun onUsernameChange(newName: String) {
        _loginState.update { it.copy(username = newName, error = null) } // 输入时清除错误
    }

    fun onPasswordChange(newPass: String) {
        _loginState.update { it.copy(password = newPass, error = null) }
    }

    fun onCaptchaChange(newCaptcha: String) {
        _loginState.update { it.copy(captcha = newCaptcha, error = null) }
    }

    fun onRememberMeChange(checked: Boolean) {
        _loginState.update { it.copy(rememberMe = checked) }
    }

    // 执行登录
    fun login(onLoginSuccess: () -> Unit = {}, onLoginError: (String) -> Unit = {}) {
        val (username, password, captcha, rememberMe) = loginState.value

        if (username.isEmpty() || password.isEmpty() || captcha.isEmpty()) {
            _loginState.update { it.copy(error = "输入格式错误") }
            return
        }
        _loginState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = AuthService.login(
                LoginRequest(
                    username = username,
                    password = password,
                    captcha = captcha
                )
            )
            result.onSuccess { res ->
                AuthStore.setUser(res.user)
                AuthStore.setToken(
                    res.accessToken,
                    res.refreshToken,
                    res.tokenType,
                    rememberMe
                )
                _loginState.update { it.copy(isLoading = false, error = null) }
                onLoginSuccess()
            }
            result.onFailure { message, _, _ ->
                _loginState.update { it.copy(isLoading = false, error = message) }
                onLoginError(message)
            }

        }

    }
}
