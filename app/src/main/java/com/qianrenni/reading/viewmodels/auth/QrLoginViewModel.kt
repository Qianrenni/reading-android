package com.qianrenni.reading.viewmodels.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qianrenni.reading.data.model.QrIdRequest
import com.qianrenni.reading.data.remote.QrLoginApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 扫码登录页面状态。
 *
 * 状态流转：扫描成功（client 就绪，弹确认框）→ 确认（isConfirmed）
 * 或取消（cancelled）；失败时 error 提示并允许重新扫描。
 */
data class QrLoginState(
    /** 网页端描述（如“网页端 Windows Chrome”），非空表示已扫描成功、待确认 */
    val client: String? = null,
    /** 当前处理的二维码票据 ID */
    val qrId: String = "",
    val isLoading: Boolean = false,
    val isConfirmed: Boolean = false,
    val isCancelled: Boolean = false,
    val error: String? = null,
)

class QrLoginViewModel(
    private val qrLoginApi: QrLoginApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _qrState = MutableStateFlow(QrLoginState())
    val qrState = _qrState.asStateFlow()

    /** 扫描到合法二维码后上报，获取网页端信息供确认框展示 */
    fun scan(qrId: String) {
        if (_qrState.value.qrId == qrId && _qrState.value.isLoading) {
            return // 同一张码重复回调，忽略
        }
        _qrState.value = QrLoginState(qrId = qrId, isLoading = true)
        viewModelScope.launch(ioDispatcher) {
            qrLoginApi.scan(QrIdRequest(qrId)).fold(
                // 后端 data 恒为对象，正常走 Success；Empty 仅为兜底
                onSuccess = { res ->
                    _qrState.update { it.copy(isLoading = false, client = res.client) }
                },
                onEmpty = {
                    _qrState.update { it.copy(isLoading = false, client = null) }
                },
                onFailure = { message, _, _ ->
                    // 失败清空 qrId，允许用户重新对准二维码再次触发扫描
                    _qrState.value = QrLoginState(error = message)
                },
            )
        }
    }

    /** 用户在手机上确认登录 */
    fun confirm(onConfirmed: () -> Unit = {}) {
        val qrId = _qrState.value.qrId
        if (qrId.isBlank() || _qrState.value.isLoading) return
        _qrState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch(ioDispatcher) {
            // confirm 响应为 ResponseModel.Empty（data=null → Empty），成功路径走 onEmpty
            qrLoginApi.confirm(QrIdRequest(qrId)).fold(
                onSuccess = { confirmed(onConfirmed) },
                onEmpty = { confirmed(onConfirmed) },
                onFailure = { message, _, _ ->
                    _qrState.update { it.copy(isLoading = false, error = message) }
                },
            )
        }
    }

    /** 用户在手机上取消本次登录 */
    fun cancel(onCancelled: () -> Unit = {}) {
        val qrId = _qrState.value.qrId
        if (qrId.isBlank() || _qrState.value.isLoading) {
            onCancelled()
            return
        }
        _qrState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch(ioDispatcher) {
            qrLoginApi.cancel(QrIdRequest(qrId)).fold(
                onSuccess = { cancelled(onCancelled) },
                onEmpty = { cancelled(onCancelled) },
                onFailure = { message, _, _ ->
                    _qrState.update { it.copy(isLoading = false, error = message) }
                },
            )
        }
    }

    private fun confirmed(onConfirmed: () -> Unit) {
        _qrState.update { it.copy(isLoading = false, isConfirmed = true) }
        onConfirmed()
    }

    private fun cancelled(onCancelled: () -> Unit) {
        _qrState.update { it.copy(isLoading = false, isCancelled = true) }
        onCancelled()
    }

    /** 重置状态（重新扫描/退出页面时调用） */
    fun reset() {
        _qrState.value = QrLoginState()
    }
}
