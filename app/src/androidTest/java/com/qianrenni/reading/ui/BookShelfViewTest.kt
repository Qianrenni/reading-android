package com.qianrenni.reading.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import com.qianrenni.reading.FakeBookApi
import com.qianrenni.reading.FakeReadingProgressApi
import com.qianrenni.reading.FakeShelfApi
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.navigation.Home
import com.qianrenni.reading.navigation.NavigationState
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.testBook
import com.qianrenni.reading.testProgress
import com.qianrenni.reading.testShelfItem
import com.qianrenni.reading.viewmodels.book.ShelfViewModel
import com.qianrenni.reading.views.book.BookShelfView
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 书架的 Compose UI 测试。
 * 通过注入 Fake API 隔离网络；覆盖书架列表渲染与删除。
 */
class BookShelfViewTest {

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

    private fun setContent(
        shelfApi: FakeShelfApi = FakeShelfApi(),
        progressApi: FakeReadingProgressApi = FakeReadingProgressApi(),
        bookApi: FakeBookApi = FakeBookApi()
    ) {
        val vm = ShelfViewModel(bookApi, progressApi, shelfApi)
        composeRule.setContent { MaterialTheme { BookShelfView(navigator(), vm) } }
    }

    @Test
    fun rendersShelfItems() {
        val shelfApi = FakeShelfApi().apply {
            shelfResult = NetworkResult.Success(listOf(testShelfItem(bookId = 1)))
        }
        val progressApi = FakeReadingProgressApi().apply {
            progressResult = NetworkResult.Success(listOf(testProgress(bookId = 1)))
        }
        val bookApi = FakeBookApi().apply {
            booksByIdsResult = NetworkResult.Success(arrayOf(testBook(1, "斗破苍穹")))
        }
        setContent(shelfApi, progressApi, bookApi)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("斗破苍穹")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("斗破苍穹").assertIsDisplayed()
        composeRule.onNodeWithText("作者").assertIsDisplayed()
        composeRule.onNodeWithText("继续阅读").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("删除").assertIsDisplayed()
    }

    @Test
    fun deleteRemovesItemFromShelf() {
        val shelfApi = FakeShelfApi().apply {
            shelfResult = NetworkResult.Success(listOf(testShelfItem(bookId = 1)))
        }
        val progressApi = FakeReadingProgressApi().apply {
            progressResult = NetworkResult.Success(listOf(testProgress(bookId = 1)))
        }
        val bookApi = FakeBookApi().apply {
            booksByIdsResult = NetworkResult.Success(arrayOf(testBook(1, "斗破苍穹")))
        }
        setContent(shelfApi, progressApi, bookApi)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("斗破苍穹")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("删除").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("斗破苍穹")).fetchSemanticsNodes().isEmpty()
        }
        assertTrue(shelfApi.removeCalled)
        assertTrue(shelfApi.lastRemovedBookId == 1)
    }
}
