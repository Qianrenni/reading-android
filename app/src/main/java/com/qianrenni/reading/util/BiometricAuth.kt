package com.qianrenni.reading.util

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 指纹（生物识别）能力封装。
 *
 * 只允许生物识别（[BiometricManager.Authenticators.BIOMETRIC_WEAK]：指纹/面容），
 * 不含设备锁屏密码——「指纹解锁」若回落到密码，就失去了本功能的意义。
 */
object BiometricAuth {

    /** 可用的认证方式。 */
    const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK

    /** 设备是否具备可用的生物识别能力（存在硬件且已录入指纹/面容）。 */
    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) ==
                BiometricManager.BIOMETRIC_SUCCESS

    /**
     * 唤起系统生物识别框，挂起直到用户完成或放弃。
     *
     * @return 是否验证通过。取消、识别失败重试耗尽、系统错误都返回 false——
     * 具体原因由系统弹窗自己表达，调用方只需区分「过没过」。
     */
    suspend fun authenticate(
        activity: FragmentActivity?,
        title: String,
        subtitle: String,
        negativeButtonText: String,
    ): Boolean {
        if (activity == null || activity.isFinishing) {
            return false
        }
        return suspendCancellableCoroutine { continuation ->
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) continuation.resume(false)
                }

                override fun onAuthenticationFailed() {
                    // 单次识别失败（例如手指没放正）：系统会提示并允许重试，此处不结束流程
                }
            }
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                callback
            )
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()
            val started = runCatching { prompt.authenticate(promptInfo) }.isSuccess
            if (!started && continuation.isActive) {
                // Activity 不在前台等情况下 BiometricPrompt 会直接抛异常，按「未通过」处理
                continuation.resume(false)
            }
        }
    }
}

/** 从 Compose 的 Context 向上找到宿主 [FragmentActivity]（BiometricPrompt 只接受它）。 */
fun Context.findFragmentActivity(): FragmentActivity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) {
            return context
        }
        context = context.baseContext
    }
    return null
}
