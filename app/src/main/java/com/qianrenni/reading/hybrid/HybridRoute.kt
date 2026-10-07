package com.qianrenni.reading.hybrid

/**
 * 容器引擎类型。
 *
 * - [NATIVE]：随 APK 发布的 Compose 页面（能力最强，但发版才能更新）
 * - [H5]：WebView 渲染的 HTML 包（可用 JSBridge 复用原生能力）
 * - [RN]：React Native 渲染的 JS bundle 包（原生级交互 + 复用原生能力）
 */
enum class EngineType {
    NATIVE,
    H5,
    RN;

    companion object {
        /** 解析服务端/配置下发的引擎名；未知返回 null。 */
        fun fromWire(value: String?): EngineType? =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) }
    }
}

/** 页面实现的来源，决定解析优先级与运维排查口径。 */
enum class HybridSource {
    /** 本机已安装的下载包（扫码预发 / 更新后）——优先级最高 */
    INSTALLED,

    /** 随 APK 发布的内置兜底包 */
    BUILTIN,

    /** 服务端路由配置声明的实现 */
    CONFIG,

    /** 本地注册的 native 页面 */
    NATIVE,
}

/**
 * 一个可被容器渲染的页面描述。
 *
 * @param route 容器路由 key，客户端用 `HybridPage(route)` 打开
 * @param engine 由哪个引擎渲染
 * @param entry h5：绝对 URL 或包内入口相对路径；rn：注册的 component 名；native：native 路由 key
 * @param localFile 本地包文件绝对路径；无本地文件（远端 H5 / native）时为 null
 * @param source 该描述的来源，用于解析优先级与排查
 */
data class HybridPageSpec(
    val route: String,
    val engine: EngineType,
    val entry: String,
    val appKey: String = "",
    val versionCode: Int = 0,
    val localFile: String? = null,
    val source: HybridSource = HybridSource.CONFIG,
)

/** 路由解析结果。 */
sealed interface HybridResolution {
    /** 可以渲染。 */
    data class Resolved(val spec: HybridPageSpec) : HybridResolution

    /**
     * 无法渲染：可能是路由未注册、或声明的引擎在本机不可用（如 RN 运行时未就绪）。
     * UI 需要把这个原因如实展示出来，而不是白屏。
     */
    data class Unavailable(val route: String, val reason: String) : HybridResolution
}

/**
 * 容器候选集：同一条 route 的四种来源，由仓库层收集后交给 [HybridRouteResolver] 决策。
 */
data class HybridCandidates(
    /** 本机已安装包 */
    val installed: List<HybridPageSpec> = emptyList(),
    /** 内置包 */
    val builtin: List<HybridPageSpec> = emptyList(),
    /** 远端配置声明 */
    val remote: List<HybridPageSpec> = emptyList(),
    /** 本地 native 注册表 */
    val native: List<HybridPageSpec> = emptyList(),
)

/**
 * 路由解析器（纯逻辑，便于单测）。
 *
 * 解析优先级（先命中先用，且该实现对应的引擎必须在本机可用）：
 * 1. 本机已安装包 —— 保证「扫码预发/更新后立刻看到新版本」
 * 2. 内置包 —— 离线兜底，避免整页不可用
 * 3. 远端配置声明 —— 运营可远程把某路由切到 H5/RN
 * 4. 本地 native 注册表 —— 最稳的兜底实现
 *
 * [forcedEngine] 来自扫码/调试入口，可越过上述优先级强制指定引擎，
 * 但仍须引擎可用（例如 RN 运行时未就绪时不能强行 RN）。
 *
 * [specRenderable] 用于更细的判断：引擎可用 ≠ 这一个实现可用（RN 实现缺本地 bundle 时不可渲染），
 * 不满足时同样按优先级继续往下选，避免进页面后才报错。
 */
object HybridRouteResolver {

    fun resolve(
        route: String,
        candidates: HybridCandidates,
        availableEngines: Set<EngineType>,
        forcedEngine: EngineType? = null,
        specRenderable: (HybridPageSpec) -> Boolean = { true },
    ): HybridResolution {
        val key = route.trim()
        if (key.isEmpty()) {
            return HybridResolution.Unavailable(route = route, reason = "路由为空")
        }
        val all = candidates.installed + candidates.builtin + candidates.remote + candidates.native
        val matched = all.filter { it.route == key }
        if (matched.isEmpty()) {
            return HybridResolution.Unavailable(route = key, reason = "未注册该路由：$key")
        }
        // 「引擎可用」与「这个实现能渲染」两个条件都要满足才算可用
        val renderable: (HybridPageSpec) -> Boolean = { spec ->
            spec.engine in availableEngines && specRenderable(spec)
        }
        if (forcedEngine != null) {
            val forced = matched.firstOrNull { it.engine == forcedEngine }
            return when {
                forced == null -> HybridResolution.Unavailable(
                    route = key,
                    reason = "该路由没有 ${forcedEngine.name} 实现"
                )

                forcedEngine !in availableEngines -> HybridResolution.Unavailable(
                    route = key,
                    reason = "${forcedEngine.name} 引擎在本机不可用"
                )

                !specRenderable(forced) -> HybridResolution.Unavailable(
                    route = key,
                    reason = "${forcedEngine.name} 实现暂时无法渲染（缺少本地包）"
                )

                else -> HybridResolution.Resolved(forced)
            }
        }
        // 按优先级顺序找第一个「引擎可用且该实现可渲染」的实现
        val ordered = candidates.installed.filter { it.route == key } +
                candidates.builtin.filter { it.route == key } +
                candidates.remote.filter { it.route == key } +
                candidates.native.filter { it.route == key }
        val usable = ordered.firstOrNull(renderable)
        if (usable != null) {
            return HybridResolution.Resolved(usable)
        }
        val declaredEngines = matched.map { it.engine }.distinct().joinToString("/") { it.name }
        return HybridResolution.Unavailable(
            route = key,
            reason = "路由已声明但引擎不可用（声明：$declaredEngines）"
        )
    }
}
