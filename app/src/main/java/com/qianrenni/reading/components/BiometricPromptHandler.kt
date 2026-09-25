package com.qianrenni.reading.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qianrenni.reading.util.BiometricAuth
import com.qianrenni.reading.util.SnackBarManager
import com.qianrenni.reading.util.findFragmentActivity
import com.qianrenni.reading.viewmodels.auth.BiometricViewModel
import com.qianrenni.reading.viewmodels.auth.biometricPromptText

/**
 * 指纹解锁的统一落地：观察 [BiometricViewModel] 的待验证请求 → 唤起系统指纹框 →
 * 回报结果；顺带展示一次性提示。
 *
 * 登录页与个人中心共用，避免两处各写一遍「弹框 → 回报 → 提示」的样板。
 * 界面只需要在点击入口时调用 `requestLogin()` / `requestEnable()`。
 */
@Composable
fun BiometricPromptHandler(viewModel: BiometricViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findFragmentActivity()

    LaunchedEffect(state.pendingAction) {
        val action = state.pendingAction ?: return@LaunchedEffect
        val text = biometricPromptText(action)
        val passed = BiometricAuth.authenticate(
            activity = activity,
            title = text.title,
            subtitle = text.subtitle,
            negativeButtonText = text.negativeButton
        )
        if (passed) {
            viewModel.onPromptSucceeded()
        } else {
            viewModel.onPromptFailed()
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            SnackBarManager.showMessage(message)
            viewModel.clearMessage()
        }
    }
}
