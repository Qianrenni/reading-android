package com.qianrenni.reading.viewmodels.book

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qianrenni.reading.data.api.BookService
import com.qianrenni.reading.data.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    data class UiState(
        val categories: List<String> = emptyList(),
        val books: List<Book> = emptyList(),
        val selectedCategory: String = "",
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val errorMessage: String = "",
        val scrollToTop: Boolean = false,
        // 搜索相关
        val searchQuery: String = "",
        val searchResults: List<Book> = emptyList(),
        val isSearching: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 缓存管理
    private val categoryBooksCache = mutableMapOf<String, MutableList<Book>>()
    private val categoryCursors = mutableMapOf<String, Int>()
    private val categoryFinished = mutableMapOf<String, Boolean>()

    // 搜索防抖任务
    private var searchJob: Job? = null

    init {
        loadCategories()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        // 取消上一次搜索任务
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSearching = true) }
            delay(300) // 防抖 300ms
            Log.d("HomeVM", "Searching books with query: $query")

            val result = BookService.searchBooks(query)
            result.onSuccess { books ->
                _uiState.update {
                    it.copy(
                        searchResults = books.toList(),
                        isSearching = false
                    )
                }
            }
            result.onFailure { message, _, _ ->
                _uiState.update {
                    it.copy(
                        searchResults = emptyList(),
                        isSearching = false,
                        isError = true,
                        errorMessage = message
                    )
                }
                Log.e("HomeVM", "Search books failed: $message")
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false,
                scrollToTop = true
            )
        }
    }

    fun selectCategory(category: String) {
        val current = _uiState.value
        if (category == current.selectedCategory) return

        _uiState.value = current.copy(
            selectedCategory = category,
            books = categoryBooksCache[category] ?: emptyList(),
            scrollToTop = true,
            isError = false,
            errorMessage = ""
        )
        // 如果该分类未加载过，自动加载
        if (category.isEmpty() || categoryBooksCache[category].isNullOrEmpty()) {
            loadBooksByCategory(category, offset = 0)
        }
    }

    fun loadMoreBooks() {
        val state = _uiState.value
        val category = state.selectedCategory

        if (category.isEmpty() || state.isLoading || categoryFinished[category] == true) return

        loadBooksByCategory(category, offset = categoryCursors[category] ?: 0)
    }

    private fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, isError = false) }
            val result = BookService.getCategories()
            result.onSuccess { categories ->
                val sorted = categories.sortedBy { it.length }
                _uiState.update {
                    it.copy(
                        categories = sorted,
                        isLoading = false,
                        selectedCategory = "", //
                        isError = false
                    )
                }
                selectCategory(sorted.first())
            }
            result.onFailure { message, _, _ ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isError = true,
                        errorMessage = message
                    )
                }
                Log.e("HomeVM", "Load categories failed $message")
            }
        }
    }

    private fun loadBooksByCategory(category: String, offset: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, isError = false) }

            val result = BookService.getBooksByCategory(category, offset, LIMIT)
            result.onSuccess { books ->
                if (books.isNotEmpty()) {
                    val cache = categoryBooksCache.getOrPut(category) { mutableListOf() }
                    val newBooks = books.filter { newBook ->
                        cache.none { it.id == newBook.id }
                    }
                    cache.addAll(newBooks)
                    categoryCursors[category] = offset + books.size

                    if (category == _uiState.value.selectedCategory) {
                        _uiState.update { item ->
                            item.copy(
                                books = categoryBooksCache[category] ?: emptyList(),
                                isLoading = false,
                                isError = false
                            )
                        }
                    }
                } else {
                    // 标记分类已加载完毕
                    categoryFinished[category] = true
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
            result.onFailure { message, _, _ ->  // 修复参数
                _uiState.update { item ->
                    item.copy(
                        isLoading = false,
                        isError = true,
                        errorMessage = message
                    )
                }
                Log.e("HomeVM", "Load books failed: $message")
            }
        }
    }

    fun resetScrollFlag() {
        _uiState.update { it.copy(scrollToTop = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(isError = false, errorMessage = "") }
    }

    companion object {
        private const val LIMIT = 10
    }
}