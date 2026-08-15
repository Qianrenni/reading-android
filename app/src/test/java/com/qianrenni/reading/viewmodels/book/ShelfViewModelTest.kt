package com.qianrenni.reading.viewmodels.book

import com.qianrenni.reading.FakeBookApi
import com.qianrenni.reading.FakeReadingProgressApi
import com.qianrenni.reading.FakeShelfApi
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
class ShelfViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun loadedViewModel(): ShelfViewModel {
        val shelfApi = FakeShelfApi().apply {
            shelfResult = NetworkResult.Success(listOf(ShelfItem(1), ShelfItem(2)))
        }
        val progressApi = FakeReadingProgressApi().apply { progressResult = NetworkResult.Success(emptyList()) }
        val bookApi = FakeBookApi().apply {
            booksByIdsResult = NetworkResult.Success(arrayOf(testBook(1), testBook(2)))
        }
        return ShelfViewModel(bookApi, progressApi, shelfApi, testDispatcher)
    }

    @Test
    fun `loadShelf populates shelf items and books`() = runTest(testDispatcher) {
        val vm = loadedViewModel()

        vm.loadShelf()
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.shelfItems.size)
        assertEquals(2, vm.uiState.value.books.size)
        assertFalse(vm.uiState.value.pageStatus.isLoading)
    }

    @Test
    fun `removeFromShelf removes item on success`() = runTest(testDispatcher) {
        val vm = loadedViewModel()
        vm.loadShelf()
        advanceUntilIdle()

        vm.removeFromShelf(1)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.shelfItems.size)
        assertTrue(vm.uiState.value.shelfItems.none { it.bookId == 1 })
    }
}
