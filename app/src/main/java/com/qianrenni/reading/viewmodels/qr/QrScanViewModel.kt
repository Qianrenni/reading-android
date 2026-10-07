package com.qianrenni.reading.viewmodels.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qianrenni.reading.data.model.QrLoginAction
import com.qianrenni.reading.data.remote.BundleApi
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.data.remote.QrLoginApi
import com.qianrenni.reading.data.repository.AuthRepository
import com.qianrenni.reading.data.repository.BundleInstallResult
import com.qianrenni.reading.data.repository.HybridBundleRepository
import com.qianrenni.reading.util.BundlePayload
import com.qianrenni.reading.util.QrLoginPayload
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 扫码结果状态。
 *
 * 扫码入口现在承担两件事，靠二维码内容区分：
 * - **登录码**（`gugareading://qr-login?...`）：扫到即上报，随后弹「确认登录」；
 * - **页面包码**（`gugareading://bundle?...`）：扫到后弹「下载并打开」，确认后拉取包信息、
 *   下载校验安装，再交给容器渲染（[installedRoute]）。
 *
 * @param ticket 已识别并上报后端的登录票据；为空表示还没有登录码
 * @param isSubmitting 正在与后端交互（登录上报 / 确认 / 包下载）
 * @param message 一次性提示文案，展示后由 UI 调用 [QrScanViewModel.clearMessage] 消费
 * @param isConfirmed 登录已在手机上确认，UI 提示后退出页面
 * @param pendingBundle 扫到的页面包，等待用户确认下载
 * @param installProgress 页面包下载进度（0f..1f）
 * @param installedRoute 页面包安装完成后的容器路由，UI 导航过去并消费掉
 */
data class QrScanState(
    val ticket: String = "",
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val isConfirmed: Boolean = false,
    val pendingBundle: BundlePayload.Request? = null,
    val installProgress: Float = 0f,
    val installedRoute: String? = null
) {
    /** 是否需要弹出「确认登录网页端」对话框。 */
    val isConfirming: Boolean
        get() = ticket.isNotEmpty() && !isConfirmed
}

/**
 * 扫码 ViewModel：把「相机识别 → 分发（登录 / 页面包）→ 后端交互」串成状态机。
 *
 * 重复识别由 [onQrDetected] 内部的闸门挡掉——相机每秒会识别到同一张码多次，
 * 只有第一次需要处理，否则会对同一个票据/包反复请求。
 */
class QrScanViewModel(
    private val qrLoginApi: QrLoginApi,
    private val authRepository: AuthRepository,
    private val bundleApi: BundleApi,
    private val bundleRepository: HybridBundleRepository,
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
     * 依次尝试登录码与页面包码；都不是本系统的码时只提示不锁定，用户可以继续对准正确的二维码。
     */
    fun onQrDetected(raw: String) {
        val current = _state.value
        if (current.isConfirming || current.isSubmitting || current.isConfirmed ||
            current.pendingBundle != null
        ) {
            return
        }
        // 优先按登录码解析：两者 scheme 相同、host 不同，不会互相误判
        val ticket = QrLoginPayload.parse(raw)
        if (ticket != null) {
            submit(ticket, QrLoginAction.SCAN)
            return
        }
        val bundleRequest = BundlePayload.parse(raw)
        if (bundleRequest != null) {
            // 包码不立刻发请求：先让用户确认要下载哪个版本
            _state.update { it.copy(pendingBundle = bundleRequest, message = null) }
            return
        }
        _state.update { it.copy(message = "请扫描本系统的登录码或页面包二维码") }
    }

    /** 用户放弃安装本次扫到的页面包。 */
    fun cancelBundle() {
        _state.update { it.copy(pendingBundle = null, installProgress = 0f) }
    }

    /**
     * 确认安装扫到的页面包：按 (appKey, version) 取服务端信息 → 下载校验 → 安装 → 交给容器渲染。
     *
     * 刻意不做「先激活再下载」：扫码预发的语义就是「只在这台机器上看某个版本」，
     * 不影响其它用户拿到的生效版本。
     */
    fun confirmBundleInstall() {
        val request = _state.value.pendingBundle ?: return
        if (_state.value.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, installProgress = 0f, message = null) }
        viewModelScope.launch(ioDispatcher) {
            val infoResult = bundleApi.info(request.appKey, request.versionCode)
            val info = (infoResult as? NetworkResult.Success)?.data
            if (info == null) {
                val reason = (infoResult as? NetworkResult.Failure)?.message ?: "无法获取页面包信息"
                _state.update { it.copy(isSubmitting = false, pendingBundle = null, message = reason) }
                return@launch
            }
            when (val installed = bundleRepository.install(info) { progress ->
                _state.update { it.copy(installProgress = progress) }
            }) {
                is BundleInstallResult.Installed -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        pendingBundle = null,
                        installProgress = 0f,
                        installedRoute = installed.bundle.route,
                        message = "页面包 v${installed.bundle.versionCode} 已就绪"
                    )
                }

                is BundleInstallResult.Failed -> _state.update {
                    it.copy(isSubmitting = false, pendingBundle = null, installProgress = 0f, message = installed.message)
                }
            }
        }
    }

    /** UI 完成导航后消费掉安装结果，避免返回本页时重复跳转。 */
    fun consumeInstalledRoute(): String? {
        val route = _state.value.installedRoute ?: return null
        _state.update { it.copy(installedRoute = null) }
        return route
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
