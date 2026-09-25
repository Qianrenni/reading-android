package com.qianrenni.reading.viewmodels.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qianrenni.reading.data.repository.AuthRepository
import com.qianrenni.reading.data.repository.BiometricLoginRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 需要用户指纹确认的动作。 */
enum class BiometricAction {
    /** 登录页：指纹解锁登录 */
    LOGIN,

    /** 个人中心：开启指纹解锁（签发凭据前先确认是本人在操作） */
    ENABLE
}

/** 系统指纹框的文案。 */
data class BiometricPromptText(
    val title: String,
    val subtitle: String,
    val negativeButton: String
)

/** 按动作给出指纹框文案（纯函数，便于单测）。 */
fun biometricPromptText(action: BiometricAction): BiometricPromptText = when (action) {
    BiometricAction.LOGIN -> BiometricPromptText(
        title = "指纹解锁登录",
        subtitle = "验证指纹后自动登录",
        negativeButton = "使用密码登录"
    )

    BiometricAction.ENABLE -> BiometricPromptText(
        title = "开启指纹解锁",
        subtitle = "验证指纹以确认本人操作",
        negativeButton = "取消"
    )
}

/**
 * 指纹解锁的状态。
 *
 * @param pendingAction 非空表示界面需要唤起系统指纹框，随后回报验证结果；
 *   把「何时弹框」建模成状态而不是让界面直接调系统 API，ViewModel 才能脱离 Activity 单测。
 */
data class BiometricUiState(
    val isBusy: Boolean = false,
    val pendingAction: BiometricAction? = null,
    val message: String? = null
)

/**
 * 指纹解锁 ViewModel：串起「指纹校验 → 设备凭据换令牌 / 签发凭据」。
 *
 * 设备能力探测通过 [isBiometricAvailable] 注入（生产环境由 AppContainer 传入
 * `BiometricAuth.isAvailable(context)`），使本类不依赖 Android 框架、可直接单测。
 */
class BiometricViewModel(
    private val biometricRepository: BiometricLoginRepository,
    private val authRepository: AuthRepository,
    private val isBiometricAvailable: () -> Boolean,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    /** 是否已开启指纹解锁。 */
    val isEnabled = biometricRepository.isEnabled

    private val _state = MutableStateFlow(BiometricUiState())
    val state = _state.asStateFlow()

    /** 设备是否支持生物识别（不支持时界面隐藏指纹入口）。 */
    fun isAvailable(): Boolean = isBiometricAvailable()

    /** 登录页：请求指纹解锁登录。 */
    fun requestLogin() = request(BiometricAction.LOGIN)

    /** 个人中心：请求开启指纹解锁。 */
    fun requestEnable() = request(BiometricAction.ENABLE)

    /** 个人中心：关闭指纹解锁（同时撤销服务端凭据）。 */
    fun disable() {
        if (_state.value.isBusy) {
            return
        }
        _state.update { it.copy(isBusy = true, message = null) }
        viewModelScope.launch(ioDispatcher) {
            biometricRepository.disable()
            _state.update { it.copy(isBusy = false, message = "已关闭指纹解锁") }
        }
    }

    /** 系统指纹框验证通过。 */
    fun onPromptSucceeded() {
        val action = _state.value.pendingAction ?: return
        _state.update { it.copy(pendingAction = null, isBusy = true) }
        viewModelScope.launch(ioDispatcher) {
            when (action) {
                BiometricAction.ENABLE -> {
                    val enabled = biometricRepository.enable()
                    _state.update {
                        it.copy(
                            isBusy = false,
                            message = if (enabled) {
                                "已开启指纹解锁，下次可直接用指纹登录"
                            } else {
                                "开启失败，请检查网络后重试"
                            }
                        )
                    }
                }

                BiometricAction.LOGIN -> loginWithCredential()
            }
        }
    }

    /** 系统指纹框被取消或验证失败。 */
    fun onPromptFailed() {
        if (_state.value.pendingAction == null) {
            return
        }
        _state.update { it.copy(pendingAction = null, message = "指纹验证未通过") }
    }

    /** 消费一次性提示。 */
    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun request(action: BiometricAction) {
        val current = _state.value
        if (current.isBusy || current.pendingAction != null) {
            return
        }
        if (!isBiometricAvailable()) {
            _state.update {
                it.copy(message = "当前设备未录入指纹/面容，请先在系统设置中完成录入")
            }
            return
        }
        _state.update { it.copy(pendingAction = action, message = null) }
    }

    /** 用设备凭据换令牌并写入会话；成功后登录态变化会驱动导航跳转。 */
    private suspend fun loginWithCredential() {
        val response = biometricRepository.login()
        if (response == null) {
            _state.update {
                it.copy(isBusy = false, message = "指纹登录已失效，请使用账号密码登录")
            }
            return
        }
        authRepository.setUser(response.user)
        authRepository.setToken(
            response.accessToken,
            response.refreshToken,
            response.tokenType
        )
        _state.update { it.copy(isBusy = false, message = null) }
    }
}
