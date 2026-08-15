package com.qianrenni.reading.viewmodels.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qianrenni.reading.common.CommonPageStatus
import com.qianrenni.reading.common.CommonUiState
import com.qianrenni.reading.data.api.AuthService
import com.qianrenni.reading.data.model.EmailVerifyRequest
import com.qianrenni.reading.data.model.RegisterRequest
import com.qianrenni.reading.data.model.UserRegister
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val email: String = "",
    val captcha: String = "",
    val xCaptchaId: String = "",
    val isVerifyingEmail: Boolean = false,
    override val pageStatus: CommonPageStatus = CommonPageStatus(),
) : CommonUiState

class RegisterViewModel : ViewModel() {
    private val _registerState = MutableStateFlow(RegisterState())
    val registerState = _registerState.asStateFlow()

    fun onUsernameChange(newUsername: String) {
        _registerState.update { it.copy(username = newUsername, pageStatus = it.pageStatus.down()) }
    }

    fun onPasswordChange(newPassword: String) {
        _registerState.update { it.copy(password = newPassword, pageStatus = it.pageStatus.down()) }
    }

    fun onConfirmPasswordChange(newConfirmPassword: String) {
        _registerState.update {
            it.copy(
                confirmPassword = newConfirmPassword,
                pageStatus = it.pageStatus.down()
            )
        }
    }

    fun onEmailChange(newEmail: String) {
        _registerState.update { it.copy(email = newEmail, pageStatus = it.pageStatus.down()) }
    }

    fun onCaptchaChange(newCaptcha: String) {
        _registerState.update { it.copy(captcha = newCaptcha, pageStatus = it.pageStatus.down()) }
    }

    fun verifyEmail(onSuccess: () -> Unit = {}) {
        val email = registerState.value.email

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _registerState.update { it.copy(pageStatus = it.pageStatus.error("邮箱格式不正确")) }
            return
        }

        _registerState.update { it.copy(isVerifyingEmail = true) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = AuthService.verifyEmail(EmailVerifyRequest(email = email))
            _registerState.update { it.copy(isVerifyingEmail = false) }

            result.onEmpty {
                onSuccess()
            }
        }
    }

    fun register(onSuccess: () -> Unit = {}) {
        val state = registerState.value

        // 表单验证
        if (state.username.isEmpty() || state.password.isEmpty() ||
            state.confirmPassword.isEmpty() || state.email.isEmpty() ||
            state.captcha.isEmpty()
        ) {
            _registerState.update { it.copy(pageStatus = it.pageStatus.error("请填写所有字段")) }
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _registerState.update { it.copy(pageStatus = it.pageStatus.error("邮箱格式不正确")) }
            return
        }

        if (state.password != state.confirmPassword) {
            _registerState.update { it.copy(pageStatus = it.pageStatus.error("两次输入密码不一致")) }
            return
        }

        _registerState.update { it.copy(pageStatus = it.pageStatus.loading()) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = AuthService.register(
                request = RegisterRequest(
                    user = UserRegister(
                        userName = state.username,
                        password = state.password,
                        email = state.email
                    ),
                    captcha = state.captcha
                )
            )

            _registerState.update { it.copy(pageStatus = it.pageStatus.down()) }

            result.onEmpty {
                onSuccess()
            }
            result.onFailure { message, _, _ ->
                _registerState.update { it.copy(pageStatus = it.pageStatus.error(message)) }
            }
        }
    }
}
