package com.qianrenni.reading.viewmodels.book

import com.qianrenni.reading.FakeBookApi
import com.qianrenni.reading.FakeCommentApi
import com.qianrenni.reading.data.model.BookComment
import com.qianrenni.reading.data.model.Catalog
import com.qianrenni.reading.data.model.CommentPageResult
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.testBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookInfoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadBookInfo populates book and catalog`() = runTest(testDispatcher) {
        val bookApi = FakeBookApi().apply {
            bookResult = NetworkResult.Success(testBook(1))
            catalogResult = NetworkResult.Success(arrayOf(Catalog(2, "标题", 10)))
            recommendResult = NetworkResult.Success(emptyArray())
        }
        val vm = BookInfoViewModel(bookApi, FakeCommentApi(), testDispatcher)

        vm.loadBookInfo(1)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.book?.id)
        assertEquals(1, vm.uiState.value.catalog.size)
        assertTrue(vm.uiState.value.catalog[0].title.contains("第一章"))
    }

    @Test
    fun `loadBookInfo failure sets error`() = runTest(testDispatcher) {
        val bookApi = FakeBookApi().apply {
            bookResult = NetworkResult.Failure("书籍不存在", 404)
            catalogResult = NetworkResult.Failure("目录加载失败", 500)
        }
        val vm = BookInfoViewModel(bookApi, FakeCommentApi(), testDispatcher)

        vm.loadBookInfo(1)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.pageStatus.isError)
    }

    @Test
    fun `loadReviews populates reviews`() = runTest(testDispatcher) {
        val commentApi = FakeCommentApi().apply {
            reviewsResult = NetworkResult.Success(
                CommentPageResult(items = listOf(BookComment(1, 5, 9, "u", content = "好")), total = 1)
            )
        }
        val vm = BookInfoViewModel(FakeBookApi(), commentApi, testDispatcher)

        vm.loadReviews(5)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.reviews.size)
        assertTrue(!vm.uiState.value.reviewsLoading)
    }

    @Test
    fun `createReview success reloads reviews`() = runTest(testDispatcher) {
        val commentApi = FakeCommentApi().apply {
            createReviewResult = NetworkResult.Empty()
            reviewsResult = NetworkResult.Success(CommentPageResult())
            myReviewResult = NetworkResult.Empty()
        }
        val vm = BookInfoViewModel(FakeBookApi(), commentApi, testDispatcher)

        var ok = false
        vm.createReview(5, "  不错的内容  ", onSuccess = { ok = true })
        advanceUntilIdle()

        assertTrue(ok)
    }

    @Test
    fun `createReview with blank content does nothing`() = runTest(testDispatcher) {
        val commentApi = FakeCommentApi().apply { createReviewResult = NetworkResult.Empty() }
        val vm = BookInfoViewModel(FakeBookApi(), commentApi, testDispatcher)

        var ok = false
        vm.createReview(5, "   ", onSuccess = { ok = true })
        advanceUntilIdle()

        assertTrue(!ok)
    }

    @Test
    fun `deleteReview success reloads`() = runTest(testDispatcher) {
        val commentApi = FakeCommentApi().apply {
            deleteReviewResult = NetworkResult.Empty()
            reviewsResult = NetworkResult.Success(CommentPageResult())
            myReviewResult = NetworkResult.Empty()
        }
        val vm = BookInfoViewModel(FakeBookApi(), commentApi, testDispatcher)

        var ok = false
        vm.deleteReview(5, onSuccess = { ok = true })
        advanceUntilIdle()

        assertTrue(ok)
    }
}
