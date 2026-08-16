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
import com.qianrenni.reading.viewmodels.book.HistoryViewModel
import com.qianrenni.reading.views.book.ReadingHistoryView
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 阅读历史的 Compose UI 测试。
 * 通过注入 Fake API 隔离网络；覆盖历史列表渲染、加入书架与删除。
 */
class ReadingHistoryViewTest {

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
        val vm = HistoryViewModel(bookApi, progressApi, shelfApi)
        composeRule.setContent { MaterialTheme { ReadingHistoryView(navigator(), vm) } }
    }

    @Test
    fun rendersHistoryItems() {
        val shelfApi = FakeShelfApi().apply { shelfResult = NetworkResult.Success(emptyList()) }
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
        composeRule.onNodeWithText("继续阅读").assertIsDisplayed()
        composeRule.onNodeWithText("加入书架").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("删除").assertIsDisplayed()
    }

    @Test
    fun addToShelfCallsApi() {
        val shelfApi = FakeShelfApi().apply { shelfResult = NetworkResult.Success(emptyList()) }
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
        composeRule.onNodeWithText("加入书架").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { shelfApi.addCalled }
        assertTrue(shelfApi.addCalled)
        assertTrue(shelfApi.lastAddRequest?.bookId == 1)
        // 加入书架后按钮应消失
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("加入书架")).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun deleteHistoryCallsApi() {
        val shelfApi = FakeShelfApi().apply { shelfResult = NetworkResult.Success(emptyList()) }
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
        assertTrue(progressApi.deleteCalled)
        assertTrue(progressApi.lastDeletedBookId == 1)
    }
}
