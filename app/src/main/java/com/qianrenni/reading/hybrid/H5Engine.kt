package com.qianrenni.reading.hybrid

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.qianrenni.reading.bridge.JsEvaluator
import com.qianrenni.reading.bridge.JsEvaluatorHolder
import com.qianrenni.reading.bridge.NativeBridge
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.util.openWithSystem
import com.qianrenni.reading.util.shouldOpenInWebView
import java.io.File

/**
 * H5 引擎：渲染 `h5` 类型的容器页面。
 *
 * 与通用 H5 容器（[com.qianrenni.reading.views.web.WebPageView]）的区别：
 * - **注入 JSBridge**（页面可调用 auth/api/route 等原生能力）——因此本引擎只渲染我们自己下发的包；
 * - 支持本地包（`file://` 指向应用私有目录内的入口文件），实现离线可用。
 */
class H5Engine(
    private val bridge: NativeBridge?,
    private val evaluatorHolder: JsEvaluatorHolder,
) : HybridEngine {

    override val type: EngineType = EngineType.H5

    override fun isAvailable(): Boolean = true

    @Composable
    override fun Render(spec: HybridPageSpec, navigator: Navigator, modifier: Modifier) {
        val url = resolveUrl(spec)
        HybridWebView(
            url = url,
            bridge = bridge,
            evaluatorHolder = evaluatorHolder,
            navigator = navigator,
            modifier = modifier,
        )
    }

    /**
     * 本地包用 `file://` 指向包内入口（包在应用私有目录，且页面网络统一走桥，故关闭 XHR 读文件即可）；
     * 没有本地文件时回退到 entry 声明的远端地址。
     */
    private fun resolveUrl(spec: HybridPageSpec): String {
        val local = spec.localFile
        if (local.isNullOrBlank()) return spec.entry
        val dir = File(local).parentFile ?: return spec.entry
        val entry = spec.entry.trimStart('/', '\\')
        return "file://${File(dir, entry).absolutePath}"
    }
}

/**
 * 容器专用 WebView：在通用 H5 容器基础上增加 JSBridge 注入与本地包支持。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HybridWebView(
    url: String,
    bridge: NativeBridge?,
    evaluatorHolder: JsEvaluatorHolder,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    var progress by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val holder = remember { HybridWebViewHolder() }

    BackHandler {
        val webView = holder.webView
        if (webView != null && webView.canGoBack()) webView.goBack() else navigator.goBack()
    }

    // 页面销毁时注销求值器，避免桥持有已销毁的 WebView
    DisposableEffect(Unit) {
        onDispose { evaluatorHolder.evaluator = null }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        // 本地包需要通过 file:// 加载包内入口与子资源（HTML/CSS/JS/图片）
                        allowFileAccess = true
                        // 不允许网页通过 XHR 读取本地任意文件；页面网络统一走 JSBridge
                        allowFileAccessFromFileURLs = false
                        allowContentAccess = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }
                    val evaluator = JsEvaluator { script -> evaluateJavascript(script, null) }
                    if (bridge != null) {
                        addJavascriptInterface(bridge, NativeBridge.JS_INTERFACE_NAME)
                        evaluatorHolder.evaluator = evaluator
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, target: String?, favicon: Bitmap?) {
                            errorMessage = null
                            // 页面脚本执行前注入垫片，保证页面一上来就能用 window.guga
                            if (bridge != null) view.evaluateJavascript(NativeBridge.SHIM, null)
                        }

                        override fun onPageFinished(view: WebView, target: String?) {
                            progress = 100
                            // 部分页面会在 onPageStarted 之后重设全局对象，这里再补一次（垫片幂等）
                            if (bridge != null) view.evaluateJavascript(NativeBridge.SHIM, null)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            val target = request.url.toString()
                            if (shouldOpenInWebView(target)) return false
                            return openWithSystem(view.context, target)
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError,
                        ) {
                            if (!request.isForMainFrame) return
                            errorMessage = error.description?.toString() ?: "页面加载失败"
                        }
                    }
                    holder.webView = this
                    loadUrl(url)
                }
            },
            onRelease = { webView ->
                holder.webView = null
                evaluatorHolder.evaluator = null
                webView.stopLoading()
                webView.destroy()
            },
        )

        if (progress in 1..99 && errorMessage == null) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
            )
        }

        errorMessage?.let { message ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("页面加载失败", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = {
                    errorMessage = null
                    holder.webView?.reload()
                }) { Text("重试") }
            }
        }
    }
}

/** 持有 WebView 实例：普通属性避免组合期写状态。 */
private class HybridWebViewHolder {
    var webView: WebView? = null
}
