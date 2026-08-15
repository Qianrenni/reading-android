package com.qianrenni.reading.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

/**
 * Create a navigation state that persists config changes and process death.
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>
): NavigationState {

    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        serializer = MutableStateSerializer(NavKeySerializer())
    ) {
        mutableStateOf(startRoute)
    }

    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks
        )
    }
}

/**
 * State holder for navigation state.
 *
 * @param startRoute - the start route. The user will exit the app through this route.
 * @param topLevelRoute - the current top level route
 * @param backStacks - the back stacks for each top level route
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>
) {
    var topLevelRoute: NavKey by topLevelRoute
    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }
}

/**
 * Convert NavigationState into NavEntries.
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>
): SnapshotStateList<NavEntry<NavKey>> {

    val decoratedEntries = backStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
        )
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider
        )
    }

    return stacksInUse
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
}

sealed class NavDecision {
    /** 允许继续导航 */
    object Allow : NavDecision()

    /** 取消本次导航（不执行任何跳转） */
    object Cancel : NavDecision()

    /** 重定向到另一个路由，后续拦截器会以新路由重新执行 */
    data class Redirect(val route: NavKey) : NavDecision()
}

typealias NavInterceptor = (
    target: NavKey?,       // 当前想要跳转的目标路由
    from: NavKey?,        // 当前栈顶路由（可能为 null）
    isBack: Boolean       // 是否是回退操作（goBack 触发）
) -> NavDecision

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 */
class Navigator(val state: NavigationState) {
    // 拦截器列表（按添加顺序执行）
    val currentState
        get() = state.backStacks[state.topLevelRoute]?.lastOrNull()
    private val interceptors = mutableListOf<NavInterceptor>()
    fun addInterceptor(interceptor: NavInterceptor) {
        interceptors.add(interceptor)
    }

    fun removeInterceptor(interceptor: NavInterceptor) {
        interceptors.remove(interceptor)
    }

    fun navigate(route: NavKey) {
        for (interceptor in interceptors) {
            when (val decision = interceptor(route, currentState, false)) {
                is NavDecision.Allow -> continue
                is NavDecision.Cancel -> return
                is NavDecision.Redirect -> {
                    navigate(decision.route)
                    return
                }
            }
        }
        if (route in state.backStacks.keys) {
            // This is a top level route, just switch to it.
            state.topLevelRoute = route
            state.backStacks[state.topLevelRoute]?.removeIf { !state.backStacks.keys.contains(it) }
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    /**
     * 替换当前栈顶路由（仅当栈深度 > 1 时替换，否则等同于 navigate）。
     * 若目标为顶级路由，则直接切换顶级路由。
     */
    fun replace(route: NavKey) {
        for (interceptor in interceptors) {
            when (val decision = interceptor(route, currentState, false)) {
                is NavDecision.Allow -> continue
                is NavDecision.Cancel -> return
                is NavDecision.Redirect -> {
                    replace(decision.route)
                    return
                }
            }
        }
        // 1. 目标是顶级路由 → 切换
        if (route in state.backStacks.keys) {
            state.topLevelRoute = route
            state.backStacks[state.topLevelRoute]?.removeIf { !state.backStacks.keys.contains(it) }
            return
        }

        // 2. 获取当前栈
        val currentStack = state.backStacks[state.topLevelRoute] ?: return

        // 3. 若栈深度 > 1，移除栈顶再添加新路由（替换）；否则直接添加（相当于 navigate）
        if (currentStack.size > 1) {
            currentStack.removeLastOrNull()
            currentStack.add(route)
        } else {
            currentStack.add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()
        // If we're at the base of the current route, go back to the start route stack.
        for (interceptor in interceptors) {
            when (val decision = interceptor(
                if (currentRoute == state.topLevelRoute) {
                    state.startRoute
                } else {
                    currentStack.lastOrNull()
                }, currentState, true
            )) {
                is NavDecision.Allow -> continue
                is NavDecision.Cancel -> return
                is NavDecision.Redirect -> {
                    navigate(decision.route)
                    return
                }
            }
        }
        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}
