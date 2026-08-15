package com.qianrenni.reading.viewmodels.book

import com.qianrenni.reading.FakeBookApi
import com.qianrenni.reading.FakeReadingProgressApi
import com.qianrenni.reading.FakeShelfApi
import com.qianrenni.reading.data.model.BookReadingProgress
import com.qianrenni.reading.data.model.ShelfItem
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun loadedViewModel(): HistoryViewModel {
        val progressApi = FakeReadingProgressApi().apply {
            progressResult = NetworkResult.Success(
                listOf(
                    BookReadingProgress(1, 2, 0, "2026-01-02"),
                    BookReadingProgress(2, 3, 0, "2026-01-01")
                )
            )
        }
        val shelfApi = FakeShelfApi().apply { shelfResult = NetworkResult.Success(listOf(ShelfItem(1))) }
        val bookApi = FakeBookApi().apply {
            booksByIdsResult = NetworkResult.Success(arrayOf(testBook(1), testBook(2)))
        }
        return HistoryViewModel(bookApi, progressApi, shelfApi, testDispatcher)
    }

    @Test
    fun `loadHistory populates history shelf and books`() = runTest(testDispatcher) {
        val vm = loadedViewModel()

        vm.loadHistory()
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.historyItems.size)
        assertEquals(setOf(1), vm.uiState.value.shelfIds)
        assertFalse(vm.uiState.value.pageStatus.isLoading)
    }

    @Test
    fun `deleteHistory removes item on success`() = runTest(testDispatcher) {
        val vm = loadedViewModel()
        vm.loadHistory()
        advanceUntilIdle()

        vm.deleteHistory(1)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.historyItems.size)
        assertTrue(vm.uiState.value.historyItems.none { it.bookId == 1 })
    }

    @Test
    fun `addToShelf adds id on success`() = runTest(testDispatcher) {
        val vm = loadedViewModel()
        vm.loadHistory()
        advanceUntilIdle()

        vm.addToShelf(99)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.shelfIds.contains(99))
    }
}
