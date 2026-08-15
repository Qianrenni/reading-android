package com.qianrenni.reading.viewmodels.book

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qianrenni.reading.common.CommonPageStatus
import com.qianrenni.reading.common.CommonUiState
import com.qianrenni.reading.data.api.BookService
import com.qianrenni.reading.data.model.Book
import com.qianrenni.reading.data.model.Catalog
import com.qianrenni.reading.util.indexToCN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val book: Book? = null,
    val catalog: List<Catalog> = emptyList(),
    val relatedBooks: List<Book> = emptyList(),
    val selectedTabIndex: Int = 0,
    override val pageStatus: CommonPageStatus = CommonPageStatus()
) : CommonUiState

class BookInfoViewModel : ViewModel() {


    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun loadBookInfo(bookId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(pageStatus = it.pageStatus.loading()) }

            // 并行加载书籍信息和目录
            val bookJob = async { BookService.getBookById(bookId) }
            val catalogJob = async { BookService.getCatalog(bookId) }
            val bookResult = bookJob.await()
            val catalogResult = catalogJob.await()
            bookResult.onSuccess { book ->
                // 加载相关推荐
                _uiState.update { state ->
                    state.copy(
                        book = book,
                    )
                }
                loadRecommendations(book.tags)
            }
            catalogResult.onSuccess { catalogList ->
                // Catalog 返回的是数组
                _uiState.update { state ->
                    state.copy(
                        catalog = catalogList.mapIndexed { index, it ->
                            it.copy(
                                title = "第${
                                    indexToCN(
                                        index + 1
                                    )
                                }章 ${it.title}"
                            )
                        },
                        pageStatus = state.pageStatus.down()
                    )
                }
            }
            bookResult.onFailure { text, i, throwable ->
                _uiState.update { it.copy(pageStatus = it.pageStatus.error(text)) }
            }
            catalogResult.onFailure { message, _, _ ->
                _uiState.update { it.copy(pageStatus = it.pageStatus.error(message)) }
                Log.e("BookInfoVM", "Load catalog failed: $message")
            }
        }
    }

    private fun loadRecommendations(tags: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (tags.isEmpty()) return@launch

            val result = BookService.getRecommendations(tags)
            result.onSuccess { books ->
                val currentBookId = _uiState.value.book?.id
                val filtered = books.filter { it.id != currentBookId }.toList()
                _uiState.update {
                    it.copy(relatedBooks = filtered)
                }
            }
            result.onFailure { message, _, _ ->
                Log.e("BookInfoVM", "Load recommendations failed: $message")
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }
}
