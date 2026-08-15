package com.qianrenni.reading.viewmodels.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qianrenni.reading.common.CommonPageStatus
import com.qianrenni.reading.common.CommonUiState
import com.qianrenni.reading.data.api.UserService
import com.qianrenni.reading.data.model.ForgotPasswordRequest
import com.qianrenni.reading.util.SnackBarManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgetPasswordState(
    val email: String = "",
    val captcha: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isSendingCode: Boolean = false,
    override val pageStatus: CommonPageStatus = CommonPageStatus(),
) : CommonUiState

class ForgetPasswordViewModel : ViewModel() {
    private val _forgetPasswordState = MutableStateFlow(ForgetPasswordState())
    val forgetPasswordState = _forgetPasswordState.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _forgetPasswordState.update { it.copy(email = newEmail, pageStatus = it.pageStatus.down()) }
    }

    fun onCaptchaChange(newCaptcha: String) {
        _forgetPasswordState.update {
            it.copy(
                captcha = newCaptcha,
                pageStatus = it.pageStatus.down()
            )
        }
    }

    fun onPasswordChange(newPassword: String) {
        _forgetPasswordState.update {
            it.copy(
                password = newPassword,
                pageStatus = it.pageStatus.down()
            )
        }
    }

    fun onConfirmPasswordChange(newConfirmPassword: String) {
        _forgetPasswordState.update {
            it.copy(
                confirmPassword = newConfirmPassword,
                pageStatus = it.pageStatus.down()
            )
        }
    }

    fun sendVerificationCode() {
        val email = forgetPasswordState.value.email

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _forgetPasswordState.update { it.copy(pageStatus = it.pageStatus.error("邮箱格式不正确")) }
            return
        }

        _forgetPasswordState.update {
            it.copy(
                isSendingCode = true,
                pageStatus = it.pageStatus.down()
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = UserService.sendForgotPasswordCode(email)
            _forgetPasswordState.update { it.copy(isSendingCode = false) }

            result.onEmpty {
                SnackBarManager.showMessage("验证码已发送到邮箱")
            }
            result.onFailure { message, _, _ ->
                _forgetPasswordState.update { it.copy(pageStatus = it.pageStatus.error(message)) }
            }
        }
    }

    fun resetPassword() {
        val state = forgetPasswordState.value

        // 表单验证
        if (state.email.isEmpty() || state.captcha.isEmpty() ||
            state.password.isEmpty() || state.confirmPassword.isEmpty()
        ) {
            _forgetPasswordState.update { it.copy(pageStatus = it.pageStatus.error("请填写所有字段")) }
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _forgetPasswordState.update { it.copy(pageStatus = it.pageStatus.error("邮箱格式不正确")) }
            return
        }

        if (state.password != state.confirmPassword) {
            _forgetPasswordState.update { it.copy(pageStatus = it.pageStatus.error("两次输入密码不一致")) }
            return
        }

        _forgetPasswordState.update { it.copy(pageStatus = it.pageStatus.loading()) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = UserService.resetPassword(
                ForgotPasswordRequest(
                    userAccount = state.email,
                    verifyCode = state.captcha,
                    password = state.password
                )
            )

            _forgetPasswordState.update { it.copy(pageStatus = it.pageStatus.down()) }

            result.onEmpty {
                SnackBarManager.showMessage("密码重置成功，请登录")
            }
            result.onFailure { message, _, _ ->
                _forgetPasswordState.update { it.copy(pageStatus = it.pageStatus.error(message)) }
            }
        }
    }
}
