package com.qianrenni.reading.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import com.qianrenni.reading.FakeBookApi
import com.qianrenni.reading.FakeCommentApi
import com.qianrenni.reading.data.model.CommentPageResult
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.navigation.Home
import com.qianrenni.reading.navigation.NavigationState
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.testBook
import com.qianrenni.reading.testCatalog
import com.qianrenni.reading.testComment
import com.qianrenni.reading.viewmodels.book.BookInfoViewModel
import com.qianrenni.reading.views.book.BookInfoView
import org.junit.Rule
import org.junit.Test

/**
 * 书籍详情页的 Compose UI 测试。
 * 通过注入 Fake BookApi + Fake CommentApi 隔离网络；覆盖书籍信息、目录与书评 Tab。
 */
class BookInfoViewTest {

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
        bookApi: FakeBookApi = FakeBookApi(),
        commentApi: FakeCommentApi = FakeCommentApi()
    ) {
        val vm = BookInfoViewModel(bookApi, commentApi)
        composeRule.setContent { MaterialTheme { BookInfoView(navigator(), 1, vm) } }
    }

    @Test
    fun rendersBookInfoAndTabs() {
        val bookApi = FakeBookApi().apply {
            bookResult = NetworkResult.Success(testBook(1, "斗破苍穹"))
            catalogResult = NetworkResult.Success(arrayOf(testCatalog(1)))
            recommendResult = NetworkResult.Success(arrayOf(testBook(2, "相关推荐书")))
        }
        setContent(bookApi)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("斗破苍穹")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("斗破苍穹").assertIsDisplayed()
        composeRule.onNodeWithText("作者").assertIsDisplayed()
        composeRule.onNodeWithText("书籍简介").assertIsDisplayed()
        composeRule.onNodeWithText("目录").assertIsDisplayed()
        composeRule.onNodeWithText("书评").assertIsDisplayed()
        composeRule.onNodeWithText("简介1").assertIsDisplayed()
    }

    @Test
    fun catalogTabShowsChapters() {
        val bookApi = FakeBookApi().apply {
            bookResult = NetworkResult.Success(testBook(1, "斗破苍穹"))
            catalogResult = NetworkResult.Success(arrayOf(testCatalog(1, "启程"), testCatalog(2, "拜师")))
        }
        setContent(bookApi)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("斗破苍穹")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("目录").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("启程", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("启程", substring = true).assertExists()
    }

    @Test
    fun reviewTabShowsReviews() {
        val bookApi = FakeBookApi().apply {
            bookResult = NetworkResult.Success(testBook(1, "斗破苍穹"))
            catalogResult = NetworkResult.Success(emptyArray())
        }
        val commentApi = FakeCommentApi().apply {
            reviewsResult = NetworkResult.Success(
                CommentPageResult(items = listOf(testComment(content = "很好看")), total = 1)
            )
        }
        setContent(bookApi, commentApi)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("斗破苍穹")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("书评").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("很好看")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("很好看").assertExists()
        composeRule.onNodeWithText("写书评").assertIsDisplayed()
    }
}
