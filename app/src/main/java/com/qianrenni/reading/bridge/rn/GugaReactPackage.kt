package com.qianrenni.reading.bridge.rn

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.uimanager.ViewManager
import com.qianrenni.reading.bridge.BridgeModule
import com.qianrenni.reading.bridge.BridgeProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * 把同一份能力层暴露给 RN 页面。
 *
 * 关键点是**复用 [BridgeModule] 实现**而不是给 RN 再写一套：
 * H5 经由 `@JavascriptInterface`、RN 经由 `@ReactMethod`，两条传输通道，
 * 能力实现（auth/api/route/device/bundle）只有一份，行为不会分叉。
 *
 * 页面侧用法：
 * ```js
 * import { NativeModules } from 'react-native';
 * const user = await NativeModules.GugaBridge.call('auth.getUser', {});
 * ```
 */
class GugaNativeModule(
    reactContext: ReactApplicationContext,
    private val modules: List<BridgeModule>,
    private val scope: CoroutineScope,
) : ReactContextBaseJavaModule(reactContext) {

    private val modulesByName = modules.associateBy { it.name }

    override fun getName(): String = NAME

    @ReactMethod
    fun call(method: String, paramsJson: String, promise: Promise) {
        scope.launch {
            try {
                val moduleName = method.substringBefore('.')
                val action = method.substringAfter('.', missingDelimiterValue = "")
                if (action.isEmpty()) {
                    promise.reject(NAME, "方法名格式应为 module.action：$method")
                    return@launch
                }
                val module = modulesByName[moduleName]
                if (module == null) {
                    promise.reject(NAME, "未知能力模块：$moduleName")
                    return@launch
                }
                promise.resolve(module.handle(action, BridgeProtocol.parseParams(paramsJson)).toWritable())
            } catch (e: Exception) {
                promise.reject(NAME, e.message ?: "调用失败")
            }
        }
    }

    /** 把 JSON 结果转成 RN 可跨桥传递的类型（只处理能力层实际会返回的几种）。 */
    private fun JsonElement.toWritable(): Any = when (this) {
        is JsonNull -> WritableNativeMap()
        is JsonPrimitive -> if (isString) content else booleanOrNull ?: content.toDoubleOrNull() ?: content
        is JsonObject -> WritableNativeMap().apply {
            this@toWritable.forEach { (key, value) -> putString(key, value.toString()) }
        }

        else -> toString()
    }

    companion object {
        const val NAME = "GugaBridge"
    }
}

/** 只注册上面这一个模块。 */
class GugaReactPackage(
    private val modules: List<BridgeModule>,
    private val scope: CoroutineScope,
) : ReactPackage {

    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> =
        listOf(GugaNativeModule(reactContext, modules, scope))

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> =
        emptyList()
}
