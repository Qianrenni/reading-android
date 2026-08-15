package com.qianrenni.reading.viewmodels.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qianrenni.reading.common.CommonPageStatus
import com.qianrenni.reading.common.CommonUiState
import com.qianrenni.reading.data.api.UserService
import com.qianrenni.reading.data.model.UpdatePasswordRequest
import com.qianrenni.reading.data.store.AuthStore
import com.qianrenni.reading.util.SnackBarManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdatePasswordState(
    val email: String = "",
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    override val pageStatus: CommonPageStatus = CommonPageStatus()
) : CommonUiState

class UpdatePasswordViewModel : ViewModel() {
    private val _updatePasswordState = MutableStateFlow(UpdatePasswordState())
    val updatePasswordState = _updatePasswordState.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _updatePasswordState.update { it.copy(email = newEmail, pageStatus = it.pageStatus.down()) }
    }

    fun onOldPasswordChange(newOldPassword: String) {
        _updatePasswordState.update {
            it.copy(
                oldPassword = newOldPassword,
                pageStatus = it.pageStatus.down()
            )
        }
    }

    fun onNewPasswordChange(newNewPassword: String) {
        _updatePasswordState.update {
            it.copy(
                newPassword = newNewPassword,
                pageStatus = it.pageStatus.down()
            )
        }
    }

    fun onConfirmPasswordChange(newConfirmPassword: String) {
        _updatePasswordState.update {
            it.copy(
                confirmPassword = newConfirmPassword,
                pageStatus = it.pageStatus.down()
            )
        }
    }

    fun updatePassword() {
        val state = updatePasswordState.value

        // 表单验证
        if (state.email.isEmpty() || state.oldPassword.isEmpty() ||
            state.newPassword.isEmpty() || state.confirmPassword.isEmpty()
        ) {
            _updatePasswordState.update { it.copy(pageStatus = it.pageStatus.error("请填写所有字段")) }
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _updatePasswordState.update { it.copy(pageStatus = it.pageStatus.error("邮箱格式不正确")) }
            return
        }

        if (state.newPassword != state.confirmPassword) {
            _updatePasswordState.update { it.copy(pageStatus = it.pageStatus.error("两次输入密码不一致")) }
            return
        }

        _updatePasswordState.update { it.copy(pageStatus = it.pageStatus.loading()) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = UserService.updatePassword(
                UpdatePasswordRequest(
                    userName = state.email,
                    oldPassword = state.oldPassword,
                    newPassword = state.newPassword
                )
            )

            _updatePasswordState.update { it.copy(pageStatus = it.pageStatus.down()) }

            result.onEmpty {
                // 清除用户状态
                AuthStore.clear()
                SnackBarManager.showMessage("密码修改成功，请重新登录")
            }
            result.onFailure { message, _, _ ->
                _updatePasswordState.update { it.copy(pageStatus = it.pageStatus.error(message)) }
            }
        }
    }
}
