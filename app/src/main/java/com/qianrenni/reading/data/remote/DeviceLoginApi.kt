package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.DeviceCredential
import com.qianrenni.reading.data.model.DeviceLoginResponse
import com.qianrenni.reading.data.model.DeviceTokenRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * 设备凭据登录 API（手机端「指纹解锁登录」的服务端接口）。
 *
 * 三个接口的认证要求不同，因此**不能共用一个客户端**：
 * - [create] / [revoke] 需要登录态 → 走带 Auth 插件的 [ApiClient]；
 * - [login] 恰恰是「还没有令牌」时用的 → 必须走裸客户端，否则会触发
 *   401 → 刷新失败 → 清理会话的副作用。
 */
interface DeviceLoginApi {
    /** 为当前登录用户签发设备凭据（开启指纹解锁时调用）。 */
    suspend fun create(): NetworkResult<DeviceCredential>

    /** 用设备凭据换取令牌（凭据一次性轮换）。 */
    suspend fun login(deviceToken: String): NetworkResult<DeviceLoginResponse>

    /** 撤销设备凭据（关闭指纹解锁）。 */
    suspend fun revoke(deviceToken: String): NetworkResult<Unit>
}

class DeviceLoginApiImpl(
    private val apiClient: ApiClient,
    private val bareClient: HttpClient,
    private val baseUrlProvider: () -> String,
) : DeviceLoginApi {

    override suspend fun create(): NetworkResult<DeviceCredential> {
        return apiClient.post("token/device")
    }

    override suspend fun login(deviceToken: String): NetworkResult<DeviceLoginResponse> {
        return try {
            val response = bareClient.post(baseUrlProvider() + "token/device/login") {
                contentType(ContentType.Application.Json)
                setBody(DeviceTokenRequest(deviceToken))
            }
            ResponseHandler.handleResponse<DeviceLoginResponse>(response)
        } catch (e: Exception) {
            NetworkResult.Failure(message = "网络错误:服务器连接异常", exception = e)
        }
    }

    override suspend fun revoke(deviceToken: String): NetworkResult<Unit> {
        return apiClient.post("token/device/revoke") {
            setBody(DeviceTokenRequest(deviceToken))
        }
    }
}
