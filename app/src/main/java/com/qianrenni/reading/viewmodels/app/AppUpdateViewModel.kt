package com.qianrenni.reading.viewmodels.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qianrenni.reading.data.repository.AppUpdateRepository
import com.qianrenni.reading.data.repository.UpdateCheckResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** 检查更新的界面状态。 */
sealed interface UpdateStatus {
    /** 未开始（不展示弹窗）。 */
    data object Idle : UpdateStatus

    /** 正在下载安装包以比对版本号。 */
    data class Checking(val message: String = "正在检查更新…") : UpdateStatus

    /** 正在下载安装包，[progress] 为 0f..1f。 */
    data class Downloading(val progress: Float) : UpdateStatus

    /** 已是最新版本。 */
    data class UpToDate(val versionName: String) : UpdateStatus

    /** 发现新版本，安装包已下载完成。 */
    data class Available(val versionName: String, val installedApk: File) : UpdateStatus

    /** 检查失败。 */
    data class Failed(val message: String) : UpdateStatus
}

data class AppUpdateState(
    val currentVersionName: String = "",
    val status: UpdateStatus = UpdateStatus.Idle
) {
    /** 是否正在执行耗时操作（下载中不允许关闭弹窗）。 */
    val isBusy: Boolean
        get() = status is UpdateStatus.Checking || status is UpdateStatus.Downloading
}

/**
 * 应用更新 ViewModel：驱动「检查更新」弹窗的各个状态。
 */
class AppUpdateViewModel(
    private val appUpdateRepository: AppUpdateRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(AppUpdateState())
    val state: StateFlow<AppUpdateState> = _state.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            val current = appUpdateRepository.currentVersion()
            _state.update { it.copy(currentVersionName = current.versionName) }
        }
    }

    /** 检查更新：下载远端安装包 → 解析版本号 → 与当前版本比对。 */
    fun checkForUpdate() {
        if (_state.value.isBusy) return
        _state.update { it.copy(status = UpdateStatus.Checking()) }
        viewModelScope.launch(ioDispatcher) {
            val result = appUpdateRepository.checkForUpdate { progress ->
                _state.update { it.copy(status = UpdateStatus.Downloading(progress)) }
            }
            _state.update { it.copy(status = result.toStatus()) }
        }
    }

    /** 关闭弹窗；下载过程中忽略。 */
    fun dismiss() {
        if (_state.value.isBusy) return
        _state.update { it.copy(status = UpdateStatus.Idle) }
    }

    private fun UpdateCheckResult.toStatus(): UpdateStatus = when (this) {
        is UpdateCheckResult.UpToDate -> UpdateStatus.UpToDate(current.versionName)
        is UpdateCheckResult.UpdateAvailable -> {
            val apk = appUpdateRepository.downloadedApk()
            if (apk == null) {
                UpdateStatus.Failed("更新包已失效，请重新检查")
            } else {
                UpdateStatus.Available(remote.versionName, apk)
            }
        }

        is UpdateCheckResult.Failed -> UpdateStatus.Failed(message)
    }
}
