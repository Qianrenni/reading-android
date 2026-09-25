package com.qianrenni.reading.viewmodels.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qianrenni.reading.data.model.QrLoginAction
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.data.remote.QrLoginApi
import com.qianrenni.reading.data.repository.AuthRepository
import com.qianrenni.reading.util.QrLoginPayload
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 扫码登录页状态。
 *
 * @param ticket 已识别并上报后端的票据；为空表示还在取景
 * @param isSubmitting 正在与后端交互（扫描上报 / 确认）
 * @param message 一次性提示文案，展示后由 UI 调用 [QrScanViewModel.clearMessage] 消费
 * @param isConfirmed 已在手机上确认登录，UI 提示后退出页面
 */
data class QrScanState(
    val ticket: String = "",
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val isConfirmed: Boolean = false
) {
    /** 是否需要弹出「确认登录网页端」对话框。 */
    val isConfirming: Boolean
        get() = ticket.isNotEmpty() && !isConfirmed
}

/**
 * 扫码登录网页端的 ViewModel：把「相机识别 → 后端登记 → 用户确认」串成状态机。
 *
 * 重复识别由 [onQrDetected] 内部的闸门挡掉——相机每秒会识别到同一张码多次，
 * 只有第一次需要上报，否则会对同一个票据反复请求。
 */
class QrScanViewModel(
    private val qrLoginApi: QrLoginApi,
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(QrScanState())
    val state = _state.asStateFlow()

    /** 当前登录账号名，用于确认弹窗说明「以谁的身份登录网页端」。 */
    val userName: String?
        get() = authRepository.user.value?.userName

    /**
     * 相机识别到二维码文本。
     *
     * 非本系统的登录码只提示不锁定，用户可以继续对准正确的二维码。
     */
    fun onQrDetected(raw: String) {
        val current = _state.value
        if (current.isConfirming || current.isSubmitting || current.isConfirmed) {
            return
        }
        val ticket = QrLoginPayload.parse(raw)
        if (ticket == null) {
            _state.update { it.copy(message = "请扫描本系统网页端登录页的二维码") }
            return
        }
        submit(ticket, QrLoginAction.SCAN)
    }

    /** 用户在手机上确认登录网页端。 */
    fun confirm() {
        val ticket = _state.value.ticket
        if (ticket.isEmpty()) {
            return
        }
        submit(ticket, QrLoginAction.CONFIRM)
    }

    /** 用户拒绝本次登录：通知后端置为已取消，并回到取景状态。 */
    fun cancel() {
        val ticket = _state.value.ticket
        if (ticket.isEmpty()) {
            return
        }
        _state.update { it.copy(ticket = "", message = "已取消登录") }
        viewModelScope.launch(ioDispatcher) {
            // 尽力通知后端；失败不影响本地回到取景状态（票据会自然过期）
            qrLoginApi.sendAction(ticket, QrLoginAction.CANCEL)
        }
    }

    /** 消费一次性提示。 */
    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun submit(ticket: String, action: QrLoginAction) {
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch(ioDispatcher) {
            val result = qrLoginApi.sendAction(ticket, action)
            _state.update { current ->
                when (result) {
                    is NetworkResult.Failure -> current.copy(
                        isSubmitting = false,
                        // 失败后清空票据，否则会卡在确认框上无法重扫
                        ticket = "",
                        message = result.message
                    )

                    else -> current.copy(
                        isSubmitting = false,
                        ticket = ticket,
                        isConfirmed = action == QrLoginAction.CONFIRM,
                        message = if (action == QrLoginAction.CONFIRM) "已确认，请回到网页端" else null
                    )
                }
            }
        }
    }
}
