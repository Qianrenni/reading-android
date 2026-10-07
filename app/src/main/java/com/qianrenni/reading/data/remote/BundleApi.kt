package com.qianrenni.reading.data.remote

import com.qianrenni.reading.data.model.AppBundleInfo
import io.ktor.client.request.parameter

/**
 * 容器页面包 API。
 *
 * 两个接口都只需登录态（不走权限码）：包内容本身就是客户端代码，不涉及用户隐私，
 * 但也没必要对匿名请求开放。
 */
interface BundleApi {
    /** 当前生效版本（客户端检查更新）。 */
    suspend fun active(appKey: String): NetworkResult<AppBundleInfo>

    /** 指定版本详情（扫码预发）。 */
    suspend fun info(appKey: String, versionCode: Int): NetworkResult<AppBundleInfo>
}

class BundleApiImpl(private val apiClient: ApiClient) : BundleApi {

    override suspend fun active(appKey: String): NetworkResult<AppBundleInfo> {
        return apiClient.get("system/bundle/active") {
            parameter("appKey", appKey)
        }
    }

    override suspend fun info(appKey: String, versionCode: Int): NetworkResult<AppBundleInfo> {
        return apiClient.get("system/bundle/info") {
            parameter("appKey", appKey)
            parameter("versionCode", versionCode)
        }
    }
}
