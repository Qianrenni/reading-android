package com.qianrenni.reading.views.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.util.SnackBarManager
import com.qianrenni.reading.util.openWithSystem
import com.qianrenni.reading.util.shouldOpenInWebView
import kotlinx.coroutines.launch

/**
 * 通用 H5 容器页面：在应用内部用 WebView 打开 http/https 链接。
 *
 * 具备的能力：
 * - JavaScript、DOM Storage、Cookie 全部开启，并与系统 WebView 共用登录态；
 * - 站内跳转、`window.open`、`target="_blank"` 均在当前 WebView 内打开；
 * - `tel:`、`mailto:`、`intent:` 等非网页链接交给系统应用处理；
 * - 全屏展示，不带应用内顶栏（页面自带导航），顶部仅保留细加载进度条；
 * - 系统返回键优先回退网页历史，再退出本页面。
 *
 * 入口：[com.qianrenni.reading.navigation.openWebPage]，
 * 也可直接 `navigator.navigate(WebPage(url = "...", title = "活动详情"))`。
 *
 * 说明：一个 [WebPage] 路由实例对应一个固定 [url]，切换链接请导航到新的 [WebPage]；
 * [title] 不参与界面展示，仅作为容器的无障碍描述与路由标识。
 */
@Composable
fun WebPageView(
    navigator: Navigator,
    url: String,
    title: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // WebView 实例由回调在组合外写入，用普通持有类保存（放 MutableState 会写入组合期状态）
    val holder = remember { WebViewHolder() }
    var progress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    /** 非网页链接（tel:/mailto:/intent: 等）交给系统应用；无可用应用时提示用户。 */
    fun handOffToSystem(target: String) {
        if (!openWithSystem(context, target)) {
            scope.launch { SnackBarManager.showMessage("无法打开链接：$target") }
        }
    }

    // 网页能后退时优先回退网页历史，否则退出当前页面
    BackHandler {
        val webView = holder.webView
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            navigator.goBack()
        }
    }

    // 全屏 H5：不显示应用内顶栏，仅用 Scaffold 的 padding 避开状态栏 / 导航栏
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .semantics { contentDescription = title ?: url }
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        applyWebPageSettings()
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                progress = newProgress
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                val target = request.url.toString()
                                if (shouldOpenInWebView(target)) return false
                                handOffToSystem(target)
                                return true
                            }

                            override fun onPageStarted(view: WebView, target: String?, favicon: Bitmap?) {
                                isLoading = true
                                errorMessage = null
                            }

                            override fun onPageFinished(view: WebView, target: String?) {
                                isLoading = false
                                progress = 100
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError
                            ) {
                                // 仅主文档失败才整页报错，子资源（图片等）失败不影响浏览
                                if (!request.isForMainFrame) return
                                isLoading = false
                                progress = 100
                                errorMessage = error.description?.toString() ?: "网页加载失败"
                            }
                        }
                        holder.webView = this
                        loadUrl(url)
                    }
                },
                onRelease = { webView ->
                    holder.webView = null
                    webView.stopLoading()
                    webView.destroy()
                }
            )

            if (isLoading) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0, 100) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            errorMessage?.let { message ->
                WebPageError(
                    message = message,
                    onRetry = {
                        errorMessage = null
                        isLoading = true
                        progress = 0
                        holder.webView?.reload()
                    },
                    onBack = { navigator.goBack() }
                )
            }
        }
    }
}

/** 加载失败提示：覆盖在 WebView 之上，避免销毁 WebView 导致丢失已有会话。 */
@Composable
private fun WebPageError(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "网页加载失败", style = MaterialTheme.typography.titleMedium)
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRetry) { Text("重试") }
                OutlinedButton(onClick = onBack) { Text("返回") }
            }
        }
    }
}

/** H5 页面所需的 WebView 配置：JS、DOM Storage、Cookie、缩放与混合内容。 */
@SuppressLint("SetJavaScriptEnabled")
private fun WebView.applyWebPageSettings() {
    settings.apply {
        // H5 页面依赖 JS 与本地存储（登录态、页面缓存）
        javaScriptEnabled = true
        domStorageEnabled = true
        loadsImagesAutomatically = true
        // 让页面按自身 viewport 适配屏幕宽度
        useWideViewPort = true
        loadWithOverviewMode = true
        // 后端默认地址为 http，放行 https 页面内的 http 资源，避免页面白屏
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        // window.open / target=_blank 在当前 WebView 内打开，而不是新窗口
        setSupportMultipleWindows(false)
        // 不允许网页读取本地文件（H5 无需此能力）
        allowFileAccess = false
        allowContentAccess = false
        cacheMode = WebSettings.LOAD_DEFAULT
    }
    // 与系统 WebView 共用 Cookie，保持网页内的登录态
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(this@applyWebPageSettings, true)
    }
}

/** 持有 WebView 实例：普通属性而非 MutableState，回调写入不会触发重组。 */
private class WebViewHolder {
    var webView: WebView? = null
}
