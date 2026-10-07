package com.qianrenni.reading.hybrid

import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.facebook.react.ReactInstanceManager
import com.facebook.react.ReactPackage
import com.facebook.react.ReactRootView
import com.facebook.soloader.SoLoader
import com.qianrenni.reading.data.repository.ThemeMode
import com.qianrenni.reading.navigation.Navigator
import java.io.File

/**
 * RN 运行时：按 bundle 路径持有并复用 [ReactInstanceManager]。
 *
 * 两条硬约束：
 * 1. **bundle 路径变化必须重建实例**——否则会继续跑旧 JS，扫码换版本后看不到新界面；
 * 2. **实例要复用**（不能每次进页面都新建）：RN 建实例是秒级操作，页面级重复创建必然卡白屏。
 */
class RnRuntime(private val application: Application) {

    private val lock = Any()
    private var manager: ReactInstanceManager? = null
    private var bundlePath: String? = null

    /** 取（必要时创建）该 bundle 对应的运行时实例。 */
    fun managerFor(path: String): ReactInstanceManager = synchronized(lock) {
        val current = manager
        if (current != null && bundlePath == path) {
            return current
        }
        current?.destroy()
        SoLoader.init(application, false)
        val created = ReactInstanceManager.builder()
            .setApplication(application)
            .setJSBundleFile(path)
            .setUseDeveloperSupport(false)
            .build()
        manager = created
        bundlePath = path
        created
    }

    /**
     * 预热：提前建好 JS 上下文，进页面时无需等待。
     *
     * `createReactContextInBackground()` 是 UI 线程约束的 API，因此统一切到主线程执行；
     * 预热失败不影响后续正常渲染（真打开时会重新尝试）。
     */
    fun prefetch(path: String) {
        Handler(Looper.getMainLooper()).post {
            runCatching {
                SoLoader.init(application, false)
                managerFor(path).createReactContextInBackground()
            }
        }
    }

    fun destroy() {
        synchronized(lock) {
            manager?.destroy()
            manager = null
            bundlePath = null
        }
    }
}

/**
 * RN 引擎：渲染 `rn` 类型的容器页面。
 *
 * 目前只支持**本机已安装的 bundle 文件**（`spec.localFile`）——这正是
 * 「扫码下载后端下发的 bundle 后渲染」这条链路；没有本地包时容器会如实报不可用并降级。
 */
class RnEngine(
    private val runtime: RnRuntime,
    private val packages: List<ReactPackage>,
    private val themeModeProvider: () -> ThemeMode,
) : HybridEngine {

    override val type: EngineType = EngineType.RN

    override fun isAvailable(): Boolean = true

    /** RN 实现必须先有本地 bundle 文件（扫码下载的包）才能真正渲染。 */
    override fun canRender(spec: HybridPageSpec): Boolean {
        val path = spec.localFile ?: return false
        return path.isNotBlank() && File(path).exists()
    }

    @Composable
    override fun Render(spec: HybridPageSpec, navigator: Navigator, modifier: Modifier) {
        val bundlePath = spec.localFile
        if (bundlePath.isNullOrBlank() || !File(bundlePath).exists()) {
            RenderError("RN 页面需要先下载页面包（缺少本地 bundle 文件）")
            return
        }
        var error by remember(spec.route, spec.versionCode) { mutableStateOf<String?>(null) }
        // 主题跟随全 App 单一真源（ThemeRepository），保证 RN 页面与 Native 页面观感一致
        val dark = themeModeProvider().isDark(isSystemInDarkTheme())

        if (error != null) {
            RenderError(error!!)
            return
        }

        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { context ->
                runCatching {
                    ReactRootView(context).apply {
                        startReactApplication(
                            runtime.managerFor(bundlePath),
                            spec.entry,
                            spec.toInitialProps(dark),
                        )
                    }
                }.getOrElse { throwable ->
                    error = throwable.message ?: "RN 运行时初始化失败"
                    // 失败时给一个空视图，错误信息由上面的分支展示
                    ReactRootView(context)
                }
            },
            update = { view ->
                // 主题/版本变化时通过 props 更新，避免重建实例
                if (view is ReactRootView) {
                    runCatching { view.setAppProperties(spec.toInitialProps(dark)) }
                }
            },
            onRelease = { view ->
                runCatching { (view as? ReactRootView)?.unmountReactApplication() }
            },
        )
    }

    @Composable
    private fun RenderError(message: String) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("RN 页面不可用", style = MaterialTheme.typography.titleMedium)
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    /** 注入给 JS 的初始属性：主题、路由与版本，便于页面做主题适配与版本自检。 */
    private fun HybridPageSpec.toInitialProps(isDark: Boolean): Bundle = Bundle().apply {
        putBoolean("dark", isDark)
        putString("route", route)
        putString("appKey", appKey)
        putInt("versionCode", versionCode)
    }
}
