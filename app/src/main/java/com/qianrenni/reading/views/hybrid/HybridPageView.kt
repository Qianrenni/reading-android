package com.qianrenni.reading.views.hybrid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qianrenni.reading.data.repository.HybridBundleRepository
import com.qianrenni.reading.di.appContainer
import com.qianrenni.reading.hybrid.EngineType
import com.qianrenni.reading.hybrid.HybridBundleCandidates
import com.qianrenni.reading.hybrid.HybridCandidates
import com.qianrenni.reading.hybrid.HybridEngineRegistry
import com.qianrenni.reading.hybrid.HybridResolution
import com.qianrenni.reading.hybrid.HybridRouteResolver
import com.qianrenni.reading.navigation.NativeRouteRegistry
import com.qianrenni.reading.navigation.Navigator

/**
 * 容器页面：把一条 route 解析成 native / H5 / RN 三种实现之一并渲染。
 *
 * 解析不通过时**不白屏**：如实展示原因（路由未注册 / 引擎不可用），并给出返回入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HybridPageView(
    route: String,
    title: String?,
    forcedEngine: String?,
    navigator: Navigator,
    engineRegistry: HybridEngineRegistry = appContainer().hybridEngines,
    bundleRepository: HybridBundleRepository = appContainer().hybridBundleRepository,
    nativeRoutes: NativeRouteRegistry = appContainer().nativeRoutes,
) {
    val installed by bundleRepository.installed.collectAsStateWithLifecycle()
    val builtin = appContainer().builtinHybridRoutes

    val resolution = HybridRouteResolver.resolve(
        route = route,
        candidates = HybridCandidates(
            installed = HybridBundleCandidates.fromInstalled(installed),
            builtin = builtin,
            native = nativeRoutes.specs(),
        ),
        availableEngines = engineRegistry.availableEngines(),
        forcedEngine = EngineType.fromWire(forcedEngine),
        specRenderable = { spec -> engineRegistry.canRender(spec) },
    )

    when (resolution) {
        is HybridResolution.Resolved -> {
            val spec = resolution.spec
            val engine = engineRegistry.engineFor(spec.engine)
            if (engine == null) {
                UnavailableContent(route = route, reason = "${spec.engine.name} 引擎不可用", navigator = navigator)
                return
            }
            if (spec.engine == EngineType.NATIVE) {
                // native 实现就是本地已注册的 NavKey：用 replace 换掉容器壳，避免返回栈里留一层
                val navKey = nativeRoutes.navKeyFor(spec.entry)
                LaunchedEffect(route, spec.entry) {
                    if (navKey != null) navigator.replace(navKey)
                }
                return
            }
            engine.Render(spec = spec, navigator = navigator, modifier = Modifier.fillMaxSize())
        }

        is HybridResolution.Unavailable -> {
            UnavailableContent(route = route, reason = resolution.reason, navigator = navigator)
        }
    }
}

/** 不可用兜底页：容器最常见的线上问题是「包没下发/引擎不匹配」，必须让用户与运维一眼看到原因。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnavailableContent(route: String, reason: String, navigator: Navigator) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("页面不可用") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "无法打开：$route", style = MaterialTheme.typography.titleMedium)
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = { navigator.goBack() },
                modifier = Modifier.padding(top = 16.dp),
            ) { Text("返回") }
        }
    }
}
