package com.qianrenni.reading.navigation

/**
 * 延迟注入的导航器持有者。
 *
 * [Navigator] 是组合期创建的（依赖 `NavigationState`），而 JSBridge 的能力模块在
 * `AppContainer` 里构造，两者生命周期不同，因此用这个持有者在组合建立后把导航器交出去。
 */
class NavigatorHolder {
    @Volatile
    var navigator: Navigator? = null
}
