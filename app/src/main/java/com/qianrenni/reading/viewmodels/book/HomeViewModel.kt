package com.qianrenni.reading.viewmodels.book

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qianrenni.reading.data.model.Book
import com.qianrenni.reading.data.remote.BookApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class HomeViewModel(
    private val bookApi: BookApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

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
    // 正在请求中的分类，防止同一分类并发重复请求（触底加载与切换分类可能同时触发）
    private val loadingCategories = ConcurrentHashMap.newKeySet<String>()

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

        searchJob = viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isSearching = true) }
            delay(300) // 防抖 300ms
            Log.d("HomeVM", "Searching books with query: $query")

            val result = bookApi.searchBooks(query)
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
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true, isError = false) }
            val result = bookApi.getCategories()
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
                if (sorted.isNotEmpty()) {
                    selectCategory(sorted.first())
                }
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
        // 同一分类已有请求在途时直接忽略，避免快速滚动/重复触发导致的并发重复请求
        if (!loadingCategories.add(category)) return

        // 同步置 loading，避免快速滚动时在置位前重复触发 loadMoreBooks
        _uiState.update { it.copy(isLoading = true, isError = false) }

        viewModelScope.launch(ioDispatcher) {
            try {
                val result = bookApi.getBooksByCategory(category, offset, LIMIT)
                result.onSuccess { books ->
                    if (books.isNotEmpty()) {
                        val cache = categoryBooksCache.getOrPut(category) { mutableListOf() }
                        val newBooks = books.filter { newBook -> cache.none { it.id == newBook.id } }
                        cache.addAll(newBooks)
                        categoryCursors[category] = offset + books.size

                        // 无论当前选中分类是否变化，都必须复位 isLoading：
                        // 否则在途请求完成后（此时用户已切走）loading 会一直卡在 true，
                        // 导致触底加载（loadMoreBooks / 视图中的 !isLoading 判断）永久失效。
                        // 只有请求结果属于当前选中分类时才刷新 books。
                        // toList() 生成新列表引用，确保 LazyVerticalStaggeredGrid 能感知 books 变化并重组
                        _uiState.update { item ->
                            val refresh = category == item.selectedCategory
                            item.copy(
                                books = if (refresh) {
                                    categoryBooksCache[category]?.toList() ?: emptyList()
                                } else {
                                    item.books
                                },
                                isLoading = false,
                                isError = false
                            )
                        }
                    } else {
                        // 标记分类已加载完毕
                        categoryFinished[category] = true
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
                result.onFailure { message, _, _ ->
                    // 非当前选中分类的失败不应污染当前视图的错误状态，但 isLoading 必须复位
                    _uiState.update { item ->
                        if (category == item.selectedCategory) {
                            item.copy(isLoading = false, isError = true, errorMessage = message)
                        } else {
                            item.copy(isLoading = false)
                        }
                    }
                    Log.e("HomeVM", "fetch FAILED: category=$category offset=$offset message=$message")
                }
                result.onEmpty {
                    // data == null 时 onSuccess/onFailure 都不会走，必须显式复位 isLoading，避免卡死
                    _uiState.update { it.copy(isLoading = false) }
                }
            } finally {
                loadingCategories.remove(category)
                // 兜底：无论协程以何种方式结束（含异常/取消）都保证 isLoading 复位
                _uiState.update { it.copy(isLoading = false) }
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