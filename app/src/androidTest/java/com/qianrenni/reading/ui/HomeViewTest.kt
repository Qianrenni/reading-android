package com.qianrenni.reading.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.navigation3.runtime.NavBackStack
import com.qianrenni.reading.FakeBookApi
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.navigation.Home
import com.qianrenni.reading.navigation.NavigationState
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.testBook
import com.qianrenni.reading.viewmodels.book.HomeViewModel
import com.qianrenni.reading.views.HomeView
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 首页（书城）的 Compose UI 测试。
 * 通过注入 Fake BookApi 隔离网络；覆盖分类渲染、书籍网格与搜索。
 */
class HomeViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun navigator(): Navigator {
        val state = NavigationState(
            startRoute = Home,
            topLevelRoute = mutableStateOf(Home),
            backStacks = mapOf(Home to NavBackStack(Home))
        )
        return Navigator(state)
    }

    private fun setContent(api: FakeBookApi = FakeBookApi()) {
        val vm = HomeViewModel(api)
        composeRule.setContent { MaterialTheme { HomeView(navigator(), vm) } }
    }

    @Test
    fun rendersCategoriesAndBooks() {
        val api = FakeBookApi().apply {
            categoriesResult = NetworkResult.Success(listOf("玄幻", "都市"))
            booksResult = NetworkResult.Success(arrayOf(testBook(1, "斗破苍穹"), testBook(2, "庆余年")))
        }
        setContent(api)

        // 分类加载与书籍网格均为异步，轮询 UI 树直到出现
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("玄幻")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("斗破苍穹").assertIsDisplayed()
        composeRule.onNodeWithText("庆余年").assertIsDisplayed()
        assertTrue(api.getCategoriesCalled)
    }

    @Test
    fun searchShowsResults() {
        val api = FakeBookApi().apply {
            categoriesResult = NetworkResult.Success(emptyList())
            searchResult = NetworkResult.Success(arrayOf(testBook(3, "搜索到的书")))
        }
        setContent(api)

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("harry")

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("搜索到的书")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("搜索到的书").assertIsDisplayed()
        assertTrue(api.searchCalled)
        assertTrue(api.lastSearchQuery?.contains("harry") == true)
    }
}
