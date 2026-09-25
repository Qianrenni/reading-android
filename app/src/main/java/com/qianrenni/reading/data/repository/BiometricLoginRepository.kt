package com.qianrenni.reading.data.repository

import com.qianrenni.reading.data.model.LoginResponse
import com.qianrenni.reading.data.remote.DeviceLoginApi
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.data.remote.ResponseHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 指纹解锁登录仓库：管理「设备凭据」的签发、保存、使用与撤销。
 *
 * 凭据保存在 [KeyValueStore]（生产环境为 Keystore 加密实现），
 * 且必须通过指纹校验后才会被取出使用，因此它比密码更适合长期留在本机。
 */
interface BiometricLoginRepository {
    /** 是否已开启指纹解锁（本地存有设备凭据）。 */
    val isEnabled: StateFlow<Boolean>

    /** 本地是否存有设备凭据。 */
    fun hasCredential(): Boolean

    /** 开启：向服务端申请设备凭据并保存。 */
    suspend fun enable(): Boolean

    /** 关闭：清除本地凭据并尽力撤销服务端凭据。 */
    suspend fun disable()

    /** 用设备凭据静默换取令牌（指纹校验通过后调用）。 */
    suspend fun login(): LoginResponse?
}

class BiometricLoginRepositoryImpl(
    private val store: KeyValueStore,
    private val deviceLoginApi: DeviceLoginApi,
) : BiometricLoginRepository {

    private val _isEnabled = MutableStateFlow(hasCredential())

    override val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    override fun hasCredential(): Boolean = !store.getString(KEY_DEVICE_TOKEN).isNullOrEmpty()

    override suspend fun enable(): Boolean {
        val result = deviceLoginApi.create()
        val credential = (result as? NetworkResult.Success)?.data ?: return false
        store.putString(KEY_DEVICE_TOKEN, credential.deviceToken)
        _isEnabled.value = true
        return true
    }

    override suspend fun disable() {
        val token = store.getString(KEY_DEVICE_TOKEN)
        clearCredential()
        if (!token.isNullOrEmpty()) {
            // 尽力撤销：撤销失败（如断网）也不该阻止本地关闭开关
            deviceLoginApi.revoke(token)
        }
    }

    override suspend fun login(): LoginResponse? {
        val token = store.getString(KEY_DEVICE_TOKEN) ?: return null
        return when (val result = deviceLoginApi.login(token)) {
            is NetworkResult.Success -> {
                // 服务端已轮换凭据，覆盖本地保存，保证下次仍能用
                store.putString(KEY_DEVICE_TOKEN, result.data.deviceToken)
                result.data.toLoginResponse()
            }

            is NetworkResult.Failure -> {
                // 只有服务端明确拒绝（凭据过期/被撤销）才丢弃凭据；
                // 网络故障属于暂时性问题，保留凭据以便下次重试
                if (result.code == ResponseHandler.FAILURE_CODE) {
                    clearCredential()
                }
                null
            }

            is NetworkResult.Empty -> null
        }
    }

    private fun clearCredential() {
        store.remove(KEY_DEVICE_TOKEN)
        _isEnabled.value = false
    }

    private companion object {
        const val KEY_DEVICE_TOKEN = "device_token"
    }
}
