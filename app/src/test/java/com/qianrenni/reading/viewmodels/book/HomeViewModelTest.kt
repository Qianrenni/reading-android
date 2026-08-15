package com.qianrenni.reading.viewmodels.book

import com.qianrenni.reading.data.model.Book
import com.qianrenni.reading.data.model.Catalog
import com.qianrenni.reading.data.remote.BookApi
import com.qianrenni.reading.data.remote.NetworkResult
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeBookApi(
        var categories: NetworkResult<List<String>> = NetworkResult.Success(emptyList()),
        var books: NetworkResult<Array<Book>> = NetworkResult.Success(emptyArray()),
        var search: NetworkResult<Array<Book>> = NetworkResult.Success(emptyArray()),
    ) : BookApi {
        override suspend fun getCategories() = categories
        override suspend fun getBooksByCategory(category: String, offset: Int, limit: Int, sort: String) = books
        override suspend fun searchBooks(query: String) = search
        override suspend fun getBookById(bookId: Int) = NetworkResult.Failure("n/a")
        override suspend fun getCatalog(bookId: Int) = NetworkResult.Failure("n/a")
        override suspend fun getChapter(chapterId: Int, bookId: Int) = NetworkResult.Failure("n/a")
        override suspend fun getRecommendations(query: String) = NetworkResult.Failure("n/a")
        override suspend fun getBooksByIds(bookIds: List<Int>) = NetworkResult.Failure("n/a")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun book(id: Int) = Book(id = id, name = "书$id", author = "作者", cover = "cover")

    @Test
    fun `init loads categories and selects first`() = runTest(testDispatcher) {
        val api = FakeBookApi(
            categories = NetworkResult.Success(listOf("玄幻", "都市")),
            books = NetworkResult.Success(arrayOf(book(1), book(2))),
        )
        val vm = HomeViewModel(api, testDispatcher)
        advanceUntilIdle()

        assertEquals(listOf("玄幻", "都市"), vm.uiState.value.categories)
        assertEquals("玄幻", vm.uiState.value.selectedCategory)
        assertEquals(2, vm.uiState.value.books.size)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.isError)
    }

    @Test
    fun `init with empty categories keeps loading false`() = runTest(testDispatcher) {
        val api = FakeBookApi(categories = NetworkResult.Success(emptyList()))
        val vm = HomeViewModel(api, testDispatcher)
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.categories.size)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `search success populates results`() = runTest(testDispatcher) {
        val api = FakeBookApi(
            categories = NetworkResult.Success(listOf("玄幻", "都市")),
            search = NetworkResult.Success(arrayOf(book(1)))
        )
        val vm = HomeViewModel(api, testDispatcher)
        advanceUntilIdle()

        vm.onSearchQueryChanged("三体")
        advanceUntilIdle()

        assertEquals("三体", vm.uiState.value.searchQuery)
        assertEquals(1, vm.uiState.value.searchResults.size)
        assertFalse(vm.uiState.value.isSearching)
    }

    @Test
    fun `blank query clears search results`() = runTest(testDispatcher) {
        val api = FakeBookApi()
        val vm = HomeViewModel(api, testDispatcher)
        advanceUntilIdle()

        vm.onSearchQueryChanged("x")
        advanceUntilIdle()
        vm.onSearchQueryChanged(" ")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.searchResults.isEmpty())
        assertFalse(vm.uiState.value.isSearching)
    }

    @Test
    fun `search failure clears results and sets error`() = runTest(testDispatcher) {
        val api = FakeBookApi(
            categories = NetworkResult.Success(listOf("玄幻", "都市")),
            search = NetworkResult.Failure("搜索失败")
        )
        val vm = HomeViewModel(api, testDispatcher)
        advanceUntilIdle()

        vm.onSearchQueryChanged("找不到")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.searchResults.isEmpty())
        assertTrue(vm.uiState.value.isError)
        assertEquals("搜索失败", vm.uiState.value.errorMessage)
    }

    @Test
    fun `selecting same category does not reload`() = runTest(testDispatcher) {
        var loadCount = 0
        val api = object : BookApi {
            override suspend fun getCategories() = NetworkResult.Success(listOf("玄幻", "都市"))
            override suspend fun getBooksByCategory(category: String, offset: Int, limit: Int, sort: String): NetworkResult<Array<Book>> {
                loadCount++
                return NetworkResult.Success(emptyArray())
            }

            override suspend fun searchBooks(query: String) = NetworkResult.Success(emptyArray<Book>())
            override suspend fun getBookById(bookId: Int) = NetworkResult.Failure("n/a")
            override suspend fun getCatalog(bookId: Int) = NetworkResult.Failure("n/a")
            override suspend fun getChapter(chapterId: Int, bookId: Int) = NetworkResult.Failure("n/a")
            override suspend fun getRecommendations(query: String) = NetworkResult.Failure("n/a")
            override suspend fun getBooksByIds(bookIds: List<Int>) = NetworkResult.Failure("n/a")
        }
        val vm = HomeViewModel(api, testDispatcher)
        advanceUntilIdle()

        val before = loadCount
        vm.selectCategory("玄幻")
        advanceUntilIdle()

        assertEquals(before, loadCount)
    }
}
