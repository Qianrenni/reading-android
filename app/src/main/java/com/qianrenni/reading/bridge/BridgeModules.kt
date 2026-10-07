package com.qianrenni.reading.bridge

import android.os.Build
import com.qianrenni.reading.data.remote.ApiClient
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.data.repository.AuthRepository
import com.qianrenni.reading.data.repository.HybridBundleRepository
import com.qianrenni.reading.data.repository.SessionManager
import com.qianrenni.reading.navigation.NavigatorHolder
import com.qianrenni.reading.navigation.openHybrid
import com.qianrenni.reading.util.SnackBarManager
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 把 Native 能力开放给容器页面。
 *
 * 这里刻意**复用现有实现**而不是另起一套：网络走 [ApiClient]（自动带 JWT、401 自动刷令牌、
 * 失败清理会话），登录态走 [SessionManager]/[AuthRepository]，路由走 [Navigator]。
 * 这样页面里的请求与 Native 页面的请求在网络层完全相同，行为不会漂移。
 */
class AuthBridgeModule(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
) : BridgeModule {
    override val name = "auth"

    override suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "getUser" -> {
            val user = authRepository.user.value
            if (user == null) {
                buildJsonObject { put("loggedIn", JsonPrimitive(false)) }
            } else {
                buildJsonObject {
                    put("loggedIn", JsonPrimitive(true))
                    put("id", JsonPrimitive(user.id))
                    put("userName", JsonPrimitive(user.userName))
                    put("email", JsonPrimitive(user.email))
                    put("avatar", JsonPrimitive(user.avatar))
                    put("isActive", JsonPrimitive(user.isActive))
                }
            }
        }

        "getToken" -> sessionManager.tokens()?.let { JsonPrimitive(it.accessToken) } ?: JsonNull

        else -> throw IllegalArgumentException("不支持的方法：$name.$method")
    }
}

/** 路由能力：容器页面可跳转其它容器路由 / 原生页面。 */
class RouteBridgeModule(private val navigatorHolder: NavigatorHolder) : BridgeModule {
    override val name = "route"

    override suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "push" -> {
            val navigator = navigatorHolder.navigator
                ?: throw IllegalStateException("容器尚未就绪")
            val route = params.string("route")
                ?: throw IllegalArgumentException("缺少参数 route")
            val title = params.string("title")
            // openHybrid 内部会按容器路由表解析引擎（native/h5/rn）
            val opened = navigator.openHybrid(route = route, title = title)
            if (!opened) {
                throw IllegalArgumentException("无法打开路由：$route")
            }
            JsonPrimitive(true)
        }

        "back" -> {
            val navigator = navigatorHolder.navigator
                ?: throw IllegalStateException("容器尚未就绪")
            navigator.goBack()
            JsonPrimitive(true)
        }

        else -> throw IllegalArgumentException("不支持的方法：$name.$method")
    }
}

/** 设备信息：页面可按平台/版本做差异化展示。 */
class DeviceBridgeModule(
    private val appVersionName: () -> String,
    private val appVersionCode: () -> Int,
) : BridgeModule {
    override val name = "device"

    override suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "info" -> buildJsonObject {
            put("platform", JsonPrimitive("android"))
            put("brand", JsonPrimitive(Build.BRAND))
            put("model", JsonPrimitive(Build.MODEL))
            put("systemVersion", JsonPrimitive(Build.VERSION.RELEASE))
            put("appVersionName", JsonPrimitive(appVersionName()))
            put("appVersionCode", JsonPrimitive(appVersionCode()))
        }

        else -> throw IllegalArgumentException("不支持的方法：$name.$method")
    }
}

/** 页面内提示。 */
class UiBridgeModule : BridgeModule {
    override val name = "ui"

    override suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "toast" -> {
            SnackBarManager.showMessage(params.string("message").orEmpty())
            JsonPrimitive(true)
        }

        else -> throw IllegalArgumentException("不支持的方法：$name.$method")
    }
}

/** 容器包信息：页面可展示「当前/已安装版本」，也用于自检。 */
class BundleBridgeModule(private val repository: HybridBundleRepository) : BridgeModule {
    override val name = "bundle"

    override suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "installed" -> buildJsonArray {
            repository.installed.value.forEach { bundle ->
                add(
                    buildJsonObject {
                        put("appKey", JsonPrimitive(bundle.appKey))
                        put("route", JsonPrimitive(bundle.route))
                        put("engine", JsonPrimitive(bundle.engine))
                        put("versionCode", JsonPrimitive(bundle.versionCode))
                        put("versionName", JsonPrimitive(bundle.versionName))
                    }
                )
            }
        }

        else -> throw IllegalArgumentException("不支持的方法：$name.$method")
    }
}

/**
 * 原生网络能力：页面无需自己处理登录态与令牌刷新。
 *
 * 支持 `api.request({ method, path, query, body })`，其中 `path` 与 Native 端一致（如 `book/list`），
 * 返回服务端 `ResponseModel` 的 `data` 部分。
 */
class NetworkBridgeModule(private val apiClient: ApiClient) : BridgeModule {
    override val name = "api"

    override suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "request" -> {
            val path = params.string("path")?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw IllegalArgumentException("缺少参数 path")
            val httpMethod = runCatching {
                HttpMethod.parse(params.string("method")?.uppercase() ?: "GET")
            }.getOrElse { throw IllegalArgumentException("不支持的 method：${params.string("method")}") }
            val query = params["query"] as? JsonObject
            val body = params["body"] as? JsonObject

            val applyQuery: HttpRequestBuilder.() -> Unit = {
                query?.forEach { (key, value) ->
                    (value as? JsonPrimitive)?.content?.let { parameter(key, it) }
                }
            }

            when (httpMethod) {
                HttpMethod.Get -> apiClient.get<JsonElement>(path, applyQuery)
                HttpMethod.Delete -> apiClient.delete<JsonElement>(path, applyQuery)
                HttpMethod.Post -> apiClient.post<JsonElement>(path) { body?.let { setBody(it) } }
                HttpMethod.Put -> apiClient.put<JsonElement>(path) { body?.let { setBody(it) } }
                else -> throw IllegalArgumentException("暂不支持 $httpMethod")
            }.unwrapOrThrow()
        }

        else -> throw IllegalArgumentException("不支持的方法：$name.$method")
    }
}

/** 把统一的网络结果转成页面可用的 JSON；失败时抛异常由桥回传 message。 */
private fun NetworkResult<JsonElement>.unwrapOrThrow(): JsonElement = when (this) {
    is NetworkResult.Success -> data
    is NetworkResult.Empty -> JsonNull
    is NetworkResult.Failure -> throw IllegalStateException(message)
}
