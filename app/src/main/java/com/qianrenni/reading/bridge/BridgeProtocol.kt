package com.qianrenni.reading.bridge

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 桥协议：H5/RN 页面与 Native 能力层的唯一约定。
 *
 * 请求：`{ "method": "auth.getUser", "params": { ... } }`
 * 响应：`{ "code": 0, "data": ..., "message": "" }`（code != 0 表示失败）
 *
 * 只走「一个入口 + JSON」而不是几十个 `@JavascriptInterface` 方法：
 * 方法名集中注册、集中做参数校验与日志，新增能力不用改桥本身。
 */
object BridgeProtocol {
    const val SUCCESS = 0
    const val FAILURE = 1

    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun success(data: JsonElement?): JsonObject = buildJsonObject {
        put("code", JsonPrimitive(SUCCESS))
        put("data", data ?: JsonPrimitive(""))
        put("message", JsonPrimitive(""))
    }

    fun failure(message: String): JsonObject = buildJsonObject {
        put("code", JsonPrimitive(FAILURE))
        put("message", JsonPrimitive(message))
    }

    /** 解析 params 字符串；为空/非法时给空对象（具体字段校验交给各模块）。 */
    fun parseParams(paramsJson: String?): JsonObject =
        paramsJson?.takeIf { it.isNotBlank() }
            ?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }
            ?: JsonObject(emptyMap())

}

/** 取字符串参数；缺字段/类型不符时返回 null，由各模块决定是否报错。 */
fun JsonObject.string(key: String): String? =
    runCatching { this[key]?.jsonPrimitive?.content }.getOrNull()

/** 取整型参数。 */
fun JsonObject.int(key: String): Int? =
    runCatching { this[key]?.jsonPrimitive?.content?.toIntOrNull() }.getOrNull()

/** 一个可被页面调用的能力模块。 */
interface BridgeModule {
    /** 方法名前缀，如 `auth`；页面调用 `auth.getUser`。 */
    val name: String

    /**
     * 处理一次调用。
     *
     * @return 返回给页面的数据；抛异常表示失败（异常信息会回传给页面，便于前端排查）
     */
    suspend fun handle(method: String, params: JsonObject): JsonElement
}
