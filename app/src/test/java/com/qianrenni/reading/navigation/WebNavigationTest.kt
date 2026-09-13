package com.qianrenni.reading.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebNavigationTest {

    private fun state(
        start: NavKey = Home,
        topLevel: Set<NavKey> = setOf(Home, Bookshelf, History, Profile)
    ): NavigationState = NavigationState(
        startRoute = start,
        topLevelRoute = mutableStateOf(start),
        backStacks = topLevel.associateWith { NavBackStack(it) }
    )

    @Test
    fun `openWebPage navigates with normalized url`() {
        val s = state()
        val nav = Navigator(s)

        assertTrue(nav.openWebPage("example.com/activity"))

        assertEquals(WebPage("https://example.com/activity", null), nav.currentState)
        assertEquals(2, s.backStacks[Home]?.size)
    }

    @Test
    fun `openWebPage keeps explicit title`() {
        val nav = Navigator(state())

        assertTrue(nav.openWebPage("http://49.235.107.221:8000/static/about.html", title = "关于我们"))

        assertEquals(
            WebPage("http://49.235.107.221:8000/static/about.html", "关于我们"),
            nav.currentState
        )
    }

    @Test
    fun `openWebPage ignores links that are not web pages`() {
        val s = state()
        val nav = Navigator(s)

        assertFalse(nav.openWebPage("tel:10086"))
        assertFalse(nav.openWebPage("mailto:support@example.com"))
        assertFalse(nav.openWebPage(""))

        assertEquals(Home, nav.currentState)
        assertEquals(1, s.backStacks[Home]?.size)
    }
}
