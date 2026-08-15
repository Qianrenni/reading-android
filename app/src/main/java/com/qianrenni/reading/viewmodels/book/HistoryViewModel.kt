package com.qianrenni.reading.viewmodels.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qianrenni.reading.common.CommonPageStatus
import com.qianrenni.reading.common.CommonUiState
import com.qianrenni.reading.data.api.BookService
import com.qianrenni.reading.data.api.ReadingProgressService
import com.qianrenni.reading.data.api.ShelfService
import com.qianrenni.reading.data.model.AddShelfRequest
import com.qianrenni.reading.data.model.Book
import com.qianrenni.reading.data.model.BookReadingProgress
import com.qianrenni.reading.util.SnackBarManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {

    data class UiState(
        val historyItems: List<BookReadingProgress> = emptyList(),
        val shelfIds: Set<Int> = emptySet(),
        val books: List<Book> = emptyList(),
        override val pageStatus: CommonPageStatus = CommonPageStatus(),
    ) : CommonUiState

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadHistory() {
        if (_uiState.value.pageStatus.isLoading) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(pageStatus = it.pageStatus.loading()) }
            val historyItemsJob = async { ReadingProgressService.getReadingProgress() }
            val shelfItemsJob = async { ShelfService.getShelf() }
            var historyItems: List<BookReadingProgress> = emptyList()
            historyItemsJob.await().onSuccess {
                historyItems = it.sortedBy { item -> item.bookId }
                val bookIds = historyItems.map { item -> item.bookId }
                bookIds
            }?.let { bookIds ->
                if (bookIds.isNotEmpty()) {
                    val orders =
                        historyItems.indices.sortedByDescending { historyItems[it].lastReadAt }
                    BookService.getBooksByIds(bookIds).onSuccess { books ->
                        val orderBooks = books.sortedBy { it.id }
                        _uiState.update {
                            it.copy(
                                books = orders.map { index -> orderBooks[index] },
                                historyItems = orders.map { index -> historyItems[index] })
                        }
                    }
                }
            }
            shelfItemsJob.await().onSuccess { shelfItems ->
                _uiState.update { state ->
                    state.copy(shelfIds = shelfItems.map { it.bookId }.toSet())
                }
            }
            _uiState.update { it.copy(pageStatus = it.pageStatus.down()) }
        }
    }

    fun deleteHistory(bookId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = ReadingProgressService.deleteReadingProgress(bookId)
            result.onEmpty {

                _uiState.update { state ->
                    state.copy(
                        historyItems = state.historyItems.filter { it.bookId != bookId },
                        books = state.books.filter { it.id != bookId })
                }
                SnackBarManager.showMessage("删除成功")
            }
            result.onFailure { msg, _, _ ->
                SnackBarManager.showMessage(msg)
            }
        }
    }

    fun addToShelf(bookId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val result =
                ShelfService.addToShelf(AddShelfRequest(bookId = bookId))
            result.onEmpty {
                _uiState.update { state ->
                    state.copy(shelfIds = state.shelfIds + bookId)
                }
                SnackBarManager.showMessage("添加成功")
            }
            result.onFailure { msg, _, _ ->
                SnackBarManager.showMessage(msg)
            }
        }
    }
}
