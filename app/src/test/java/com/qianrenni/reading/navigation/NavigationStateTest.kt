package com.qianrenni.reading.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationStateTest {

    private fun state(
        start: NavKey = Home,
        topLevel: Set<NavKey> = setOf(Home, Bookshelf, History, Profile)
    ): NavigationState = NavigationState(
        startRoute = start,
        topLevelRoute = mutableStateOf(start),
        backStacks = topLevel.associateWith { NavBackStack(it) }
    )

    @Test
    fun `navigate to top level switches route`() {
        val nav = Navigator(state())
        nav.navigate(Profile)
        assertEquals(Profile, nav.state.topLevelRoute)
    }

    @Test
    fun `navigate to non top level adds to current stack`() {
        val s = state()
        val nav = Navigator(s)
        nav.navigate(BookInfo(5))
        assertEquals(BookInfo(5), nav.currentState)
        assertEquals(2, s.backStacks[Home]?.size)
    }

    @Test
    fun `goBack returns to start route`() {
        val s = state()
        val nav = Navigator(s)
        nav.navigate(BookInfo(5))
        assertEquals(BookInfo(5), nav.currentState)

        nav.goBack()

        assertEquals(Home, nav.currentState)
    }

    @Test
    fun `goBack on top level switches back to start`() {
        val s = state()
        val nav = Navigator(s)
        nav.navigate(Profile)
        assertEquals(Profile, s.topLevelRoute)

        nav.goBack()

        assertEquals(Home, s.topLevelRoute)
    }

    @Test
    fun `interceptor cancel blocks navigation`() {
        val s = state()
        val nav = Navigator(s)
        nav.addInterceptor { _, _, _ -> NavDecision.Cancel }
        nav.navigate(Profile)
        assertEquals(Home, s.topLevelRoute)
    }

    @Test
    fun `interceptor allow continues navigation`() {
        val s = state()
        val nav = Navigator(s)
        nav.addInterceptor { _, _, _ -> NavDecision.Allow }
        nav.navigate(Profile)
        assertEquals(Profile, s.topLevelRoute)
    }

    @Test
    fun `interceptor redirect changes target`() {
        val s = state()
        val nav = Navigator(s)
        nav.addInterceptor { target, _, _ ->
            if (target == Profile) NavDecision.Redirect(Bookshelf) else NavDecision.Allow
        }
        nav.navigate(Profile)
        assertEquals(Bookshelf, s.topLevelRoute)
    }

    @Test
    fun `replace swaps top of stack when deep`() {
        val s = state()
        val nav = Navigator(s)
        nav.navigate(BookInfo(1))
        nav.replace(BookRead(1, 2))
        assertEquals(BookRead(1, 2), nav.currentState)
        assertEquals(2, s.backStacks[Home]?.size)
    }

    @Test
    fun `replace to top level switches route`() {
        val s = state()
        val nav = Navigator(s)
        nav.navigate(BookInfo(1))
        nav.replace(History)
        assertEquals(History, s.topLevelRoute)
    }

    @Test
    fun `currentState is null on empty stack`() {
        val s = NavigationState(
            startRoute = Home,
            topLevelRoute = mutableStateOf(Home),
            backStacks = mapOf(Home to NavBackStack())
        )
        val nav = Navigator(s)
        assertEquals(null, nav.currentState)
    }
}
