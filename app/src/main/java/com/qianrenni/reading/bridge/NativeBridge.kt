package com.qianrenni.reading.bridge

import android.webkit.JavascriptInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

/** 页面结果的回推通道：由 WebView 宿主实现（`WebView.evaluateJavascript`）。 */
fun interface JsEvaluator {
    fun evaluate(script: String)
}

/**
 * 求值器持有者：WebView 挂载时注册自己，销毁时注销。
 *
 * 桥是应用级单例，而 `evaluateJavascript` 属于具体 WebView 实例，
 * 因此用这个可变持有者把两者解耦（避免桥里长期持有已销毁的 WebView 导致泄漏）。
 */
class JsEvaluatorHolder {
    @Volatile
    var evaluator: JsEvaluator? = null

    fun evaluate(script: String) {
        evaluator?.evaluate(script)
    }
}

/**
 * 容器与 H5 页面之间的唯一出口。
 *
 * 两个刻意的设计：
 * 1. **只有一个 `@JavascriptInterface` 方法**：`call(requestId, method, paramsJson)`。
 *    方法名集中注册、集中校验与审计，新增能力不用改桥本身；
 * 2. **异步**：`call` 立即返回，结果通过 `window.__gugaBridgeResolve(id, res)` 回推。
 *    因为 `@JavascriptInterface` 方法必须在 JS 线程同步返回，而网络等能力是挂起的——
 *    统一异步就避免「同步方法 + 阻塞等待」这种会卡住 WebView 的写法。
 *
 * 安全边界：桥**只注入容器页面**（我们自己下发的 h5 包），通用 H5 容器 [com.qianrenni.reading.views.web.WebPageView]
 * 不注入，避免把登录态暴露给任意第三方网页。
 */
class NativeBridge(
    private val modules: List<BridgeModule>,
    private val scope: CoroutineScope,
    /** 当前活动的 WebView 求值器；页面未挂载时为空，回推会被安全忽略。 */
    private val evaluatorHolder: JsEvaluatorHolder,
) {
    private val modulesByName = modules.associateBy { it.name }

    @JavascriptInterface
    fun call(requestId: String, method: String, paramsJson: String) {
        scope.launch {
            val response = try {
                val moduleName = method.substringBefore('.', missingDelimiterValue = "")
                val action = method.substringAfter('.', missingDelimiterValue = "")
                if (moduleName.isEmpty() || action.isEmpty()) {
                    throw IllegalArgumentException("方法名格式应为 module.action：$method")
                }
                val module = modulesByName[moduleName]
                    ?: throw IllegalArgumentException("未知能力模块：$moduleName")
                BridgeProtocol.success(module.handle(action, BridgeProtocol.parseParams(paramsJson)))
            } catch (e: Exception) {
                BridgeProtocol.failure(e.message ?: "调用失败")
            }
            pushResult(requestId, response)
        }
    }

    private fun pushResult(requestId: String, response: JsonObject) {
        val idLiteral = BridgeProtocol.json.encodeToJsonElement(JsonPrimitive(requestId)).toString()
        val payload = BridgeProtocol.json.encodeToString(JsonObject.serializer(), response)
        // JSON 是 JS 的子集：把 id 编码成 JSON 字符串字面量即可安全内插，无需手写转义
        evaluatorHolder.evaluate("window.__gugaBridgeResolve($idLiteral, $payload)")
    }

    companion object {
        /** 注入到页面 window 上的 Native 桥对象名。 */
        const val JS_INTERFACE_NAME = "GugaNativeBridge"

        /**
         * 页面侧垫片：把 `call` 包装成 Promise，并给出常用快捷方法。
         *
         * 用 `guga.call('auth.getUser').then(...)` 即可拿到 Native 登录态；
         * 页面不做任何令牌处理，网络统一走 `api.request`（自动带 JWT 与刷新）。
         */
        val SHIM: String = """
            (function () {
              if (window.guga && window.guga.__ready) { return; }
              var seq = 0;
              var pending = {};
              window.__gugaBridgeResolve = function (id, res) {
                var handler = pending[id];
                if (!handler) { return; }
                delete pending[id];
                if (res && res.code === 0) { handler.resolve(res.data); }
                else { handler.reject(new Error((res && res.message) || 'bridge error')); }
              };
              window.guga = {
                __ready: true,
                call: function (method, params) {
                  return new Promise(function (resolve, reject) {
                    var id = 'guga_' + (++seq);
                    pending[id] = { resolve: resolve, reject: reject };
                    try {
                      GugaNativeBridge.call(String(id), String(method), JSON.stringify(params || {}));
                    } catch (e) {
                      delete pending[id];
                      reject(e);
                    }
                  });
                },
                getUser: function () { return this.call('auth.getUser'); },
                getToken: function () { return this.call('auth.getToken'); },
                device: function () { return this.call('device.info'); },
                bundles: function () { return this.call('bundle.installed'); },
                route: function (route, title) {
                  return this.call('route.push', { route: route, title: title });
                },
                back: function () { return this.call('route.back'); },
                toast: function (message) { return this.call('ui.toast', { message: message }); },
                request: function (method, path, options) {
                  var params = { method: method, path: path };
                  if (options && options.query) { params.query = options.query; }
                  if (options && options.body) { params.body = options.body; }
                  return this.call('api.request', params);
                }
              };
            })();
        """.trimIndent()
    }
}
