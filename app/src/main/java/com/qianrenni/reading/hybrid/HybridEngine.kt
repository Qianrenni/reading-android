package com.qianrenni.reading.hybrid

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.qianrenni.reading.data.model.InstalledBundle
import com.qianrenni.reading.navigation.Navigator
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 引擎宿主：容器只依赖这个契约，新增引擎（如 Flutter/小程序容器）时不动路由与页面。
 */
interface HybridEngine {
    val type: EngineType

    /** 该引擎此刻是否可用于渲染（例如 RN 需要本机存在对应的 bundle 文件）。 */
    fun isAvailable(): Boolean

    /**
     * 能否渲染这**一个具体实现**（比 [isAvailable] 更细）。
     *
     * 典型场景：RN 引擎本身可用，但某个 rn 实现没有下载到本地 bundle 文件时就渲染不了，
     * 容器应当据此降级到内置包/native，而不是进页面后弹错误。
     */
    fun canRender(spec: HybridPageSpec): Boolean = isAvailable()

    @Composable
    fun Render(spec: HybridPageSpec, navigator: Navigator, modifier: Modifier)
}

/** 引擎注册表：把「引擎可用性」收敛到一处，路由解析据此决策与降级。 */
class HybridEngineRegistry(engines: List<HybridEngine>) {

    private val engines = engines.associateBy { it.type }

    /** 当前可用于渲染的引擎集合。 */
    fun availableEngines(): Set<EngineType> =
        this.engines.values.filter { it.isAvailable() }.map { it.type }.toSet()

    /** 该实现能否被对应引擎真正渲染（含「引擎可用」这一步）。 */
    fun canRender(spec: HybridPageSpec): Boolean =
        this.engines[spec.engine]?.canRender(spec) ?: false

    fun engineFor(type: EngineType): HybridEngine? =
        this.engines[type]?.takeIf { it.isAvailable() }
}

/** 已安装包 → 路由候选（本机包优先级最高）。 */
object HybridBundleCandidates {

    fun fromInstalled(bundles: List<InstalledBundle>): List<HybridPageSpec> =
        bundles.mapNotNull { bundle ->
            val engine = EngineType.fromWire(bundle.engine) ?: return@mapNotNull null
            if (bundle.route.isBlank() || bundle.entry.isBlank()) return@mapNotNull null
            HybridPageSpec(
                route = bundle.route,
                engine = engine,
                entry = bundle.entry,
                appKey = bundle.appKey,
                versionCode = bundle.versionCode,
                localFile = bundle.filePath,
                source = HybridSource.INSTALLED,
            )
        }
}

/**
 * 内置路由清单（`assets/hybrid/builtin.json`）：随 APK 发布的兜底实现。
 *
 * 目前只用于「指向远端 H5」的兜底页（无网络时由 WebView 缓存兜底），
 * 本地 H5/RN 包请走上传 + 下载安装的路径。
 */
object BuiltinRouteManifest {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Entry(
        val route: String,
        val engine: String,
        val entry: String,
        val appKey: String = "guga-builtin",
        val versionCode: Int = 0,
    )

    /** 解析清单；格式非法时返回空列表（容器会如实报「路由未注册」而不是崩溃）。 */
    fun parse(content: String): List<HybridPageSpec> = runCatching {
        json.decodeFromString<List<Entry>>(content).mapNotNull { item ->
            val engine = EngineType.fromWire(item.engine) ?: return@mapNotNull null
            if (item.route.isBlank() || item.entry.isBlank()) return@mapNotNull null
            HybridPageSpec(
                route = item.route,
                engine = engine,
                entry = item.entry,
                appKey = item.appKey,
                versionCode = item.versionCode,
                localFile = null,
                source = HybridSource.BUILTIN,
            )
        }
    }.getOrElse { emptyList() }
}
