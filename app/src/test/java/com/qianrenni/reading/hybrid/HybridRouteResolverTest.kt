package com.qianrenni.reading.hybrid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 容器路由解析测试：优先级、降级、（扫码/调试用的）强制引擎。
 *
 * 这几个规则决定「同一条 route 到底渲染谁」，是容器最容易出错又最难在真机上排查的部分，
 * 因此全部用纯逻辑覆盖。
 */
class HybridRouteResolverTest {

    private val allEngines = setOf(EngineType.NATIVE, EngineType.H5, EngineType.RN)

    private fun spec(
        engine: EngineType,
        source: HybridSource,
        route: String = ROUTE,
    ) = HybridPageSpec(
        route = route,
        engine = engine,
        entry = engine.name.lowercase(),
        source = source,
    )

    private fun resolve(
        candidates: HybridCandidates,
        availableEngines: Set<EngineType> = allEngines,
        forcedEngine: EngineType? = null,
        route: String = ROUTE,
    ) = HybridRouteResolver.resolve(route, candidates, availableEngines, forcedEngine)

    private fun resolved(candidates: HybridCandidates, available: Set<EngineType> = allEngines) =
        (resolve(candidates, available) as HybridResolution.Resolved).spec

    private fun unavailableReason(candidates: HybridCandidates, available: Set<EngineType> = allEngines) =
        (resolve(candidates, available) as HybridResolution.Unavailable).reason

    @Test
    fun `空路由直接不可用`() {
        assertTrue(resolve(HybridCandidates(), route = "  ") is HybridResolution.Unavailable)
    }

    @Test
    fun `未注册的路由报未注册而不是白屏`() {
        val resolution = resolve(
            HybridCandidates(native = listOf(spec(EngineType.NATIVE, HybridSource.NATIVE))),
            route = "not-registered",
        )
        assertTrue(resolution is HybridResolution.Unavailable)
        assertTrue((resolution as HybridResolution.Unavailable).reason.contains("未注册"))
    }

    @Test
    fun `已安装包优先于内置包与远端配置与 native`() {
        val candidates = HybridCandidates(
            installed = listOf(spec(EngineType.RN, HybridSource.INSTALLED)),
            builtin = listOf(spec(EngineType.H5, HybridSource.BUILTIN)),
            remote = listOf(spec(EngineType.H5, HybridSource.CONFIG)),
            native = listOf(spec(EngineType.NATIVE, HybridSource.NATIVE)),
        )
        assertEquals(EngineType.RN, resolved(candidates).engine)
        assertEquals(HybridSource.INSTALLED, resolved(candidates).source)
    }

    @Test
    fun `已安装包的引擎不可用时降级到内置包`() {
        val candidates = HybridCandidates(
            installed = listOf(spec(EngineType.RN, HybridSource.INSTALLED)),
            builtin = listOf(spec(EngineType.H5, HybridSource.BUILTIN)),
            native = listOf(spec(EngineType.NATIVE, HybridSource.NATIVE)),
        )
        val available = setOf(EngineType.NATIVE, EngineType.H5)
        assertEquals(HybridSource.BUILTIN, resolved(candidates, available).source)
    }

    @Test
    fun `内置包缺失时用远端配置`() {
        val candidates = HybridCandidates(
            remote = listOf(spec(EngineType.H5, HybridSource.CONFIG)),
            native = listOf(spec(EngineType.NATIVE, HybridSource.NATIVE)),
        )
        assertEquals(HybridSource.CONFIG, resolved(candidates).source)
    }

    @Test
    fun `都没有声明时回退到 native`() {
        val candidates = HybridCandidates(native = listOf(spec(EngineType.NATIVE, HybridSource.NATIVE)))
        assertEquals(EngineType.NATIVE, resolved(candidates).engine)
    }

    @Test
    fun `路由声明了但引擎全不可用时说明声明了哪些引擎`() {
        val candidates = HybridCandidates(
            installed = listOf(spec(EngineType.RN, HybridSource.INSTALLED)),
            builtin = listOf(spec(EngineType.H5, HybridSource.BUILTIN)),
        )
        val reason = unavailableReason(candidates, available = setOf(EngineType.NATIVE))
        assertTrue(reason.contains("RN"))
        assertTrue(reason.contains("H5"))
    }

    @Test
    fun `其它路由的实现不会命中当前路由`() {
        val candidates = HybridCandidates(
            installed = listOf(spec(EngineType.RN, HybridSource.INSTALLED, route = "bookshelf")),
        )
        assertTrue(resolve(candidates) is HybridResolution.Unavailable)
    }

    @Test
    fun `强制引擎可越过优先级指定实现`() {
        val candidates = HybridCandidates(
            installed = listOf(spec(EngineType.RN, HybridSource.INSTALLED)),
            native = listOf(spec(EngineType.NATIVE, HybridSource.NATIVE)),
        )
        val forced = resolve(candidates, forcedEngine = EngineType.NATIVE) as HybridResolution.Resolved
        assertEquals(HybridSource.NATIVE, forced.spec.source)
    }

    @Test
    fun `强制引擎但该路由没有对应实现时不可用`() {
        val candidates = HybridCandidates(native = listOf(spec(EngineType.NATIVE, HybridSource.NATIVE)))
        val reason = (resolve(candidates, forcedEngine = EngineType.RN) as HybridResolution.Unavailable).reason
        assertTrue(reason.contains("RN"))
    }

    @Test
    fun `强制引擎不可用时仍然不可用（不能强行跑 RN）`() {
        val candidates = HybridCandidates(installed = listOf(spec(EngineType.RN, HybridSource.INSTALLED)))
        val available = setOf(EngineType.NATIVE, EngineType.H5)
        val reason = (resolve(candidates, available, forcedEngine = EngineType.RN) as HybridResolution.Unavailable).reason
        assertTrue(reason.contains("不可用"))
    }

    @Test
    fun `路由首尾空白会被忽略`() {
        val candidates = HybridCandidates(installed = listOf(spec(EngineType.H5, HybridSource.INSTALLED)))
        assertTrue(resolve(candidates, route = " $ROUTE ") is HybridResolution.Resolved)
    }

    @Test
    fun `已安装实现暂不可渲染时按优先级继续降级`() {
        val candidates = HybridCandidates(
            installed = listOf(spec(EngineType.RN, HybridSource.INSTALLED)),
            native = listOf(spec(EngineType.NATIVE, HybridSource.NATIVE)),
        )

        val resolution = HybridRouteResolver.resolve(
            route = ROUTE,
            candidates = candidates,
            availableEngines = allEngines,
            // 模拟「RN 引擎在，但这个 rn 实现没有本地 bundle」
            specRenderable = { it.engine != EngineType.RN },
        )

        assertEquals(HybridSource.NATIVE, (resolution as HybridResolution.Resolved).spec.source)
    }

    @Test
    fun `强制引擎但该实现不可渲染时如实说明缺少本地包`() {
        val candidates = HybridCandidates(installed = listOf(spec(EngineType.RN, HybridSource.INSTALLED)))

        val resolution = HybridRouteResolver.resolve(
            route = ROUTE,
            candidates = candidates,
            availableEngines = allEngines,
            forcedEngine = EngineType.RN,
            specRenderable = { false },
        )

        assertTrue((resolution as HybridResolution.Unavailable).reason.contains("缺少本地包"))
    }

    @Test
    fun `全部实现都不可渲染时给出引擎不可用的原因`() {
        val candidates = HybridCandidates(
            installed = listOf(spec(EngineType.RN, HybridSource.INSTALLED)),
            builtin = listOf(spec(EngineType.H5, HybridSource.BUILTIN)),
        )

        val resolution = HybridRouteResolver.resolve(
            route = ROUTE,
            candidates = candidates,
            availableEngines = allEngines,
            specRenderable = { false },
        )

        assertTrue((resolution as HybridResolution.Unavailable).reason.contains("引擎不可用"))
    }

    private companion object {
        const val ROUTE = "hybrid-demo"
    }
}
