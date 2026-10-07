package com.qianrenni.reading.navigation

import androidx.navigation3.runtime.NavKey
import com.qianrenni.reading.hybrid.EngineType
import com.qianrenni.reading.hybrid.HybridPageSpec
import com.qianrenni.reading.hybrid.HybridSource

/**
 * native 路由注册表：把「能力最强的本地页面」也纳入容器路由表。
 *
 * 这样同一条 route 可以同时存在 native / h5 / rn 三种实现，
 * 由容器按「已安装包 > 内置包 > 远端配置 > native」的优先级选择——
 * 上传一个 h5/rn 包就能把某条 native 路由切过去，不需要发版（也可随时卸包切回）。
 */
class NativeRouteRegistry(private val routes: Map<String, NavKey>) {

    fun navKeyFor(route: String): NavKey? = routes[route.trim()]

    /** 供路由解析使用的 native 候选。 */
    fun specs(): List<HybridPageSpec> = routes.keys.map { route ->
        HybridPageSpec(
            route = route,
            engine = EngineType.NATIVE,
            entry = route,
            source = HybridSource.NATIVE,
        )
    }
}

/**
 * 在应用内打开一条容器路由。
 *
 * 引擎由容器路由表解析（可显式指定，用于扫码/调试），因此调用方不需要知道
 * 这个页面最终是 native、H5 还是 RN 实现的。
 *
 * @param engine 强制指定引擎名（native/h5/rn）；null 表示交给容器决策
 * @return 是否已导航
 */
fun Navigator.openHybrid(route: String, title: String? = null, engine: String? = null): Boolean {
    val key = route.trim()
    if (key.isEmpty()) return false
    navigate(HybridPage(route = key, title = title, engine = engine))
    return true
}
