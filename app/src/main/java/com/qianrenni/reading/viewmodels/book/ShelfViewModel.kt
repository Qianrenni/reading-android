package com.qianrenni.reading.viewmodels.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qianrenni.reading.common.CommonPageStatus
import com.qianrenni.reading.common.CommonUiState
import com.qianrenni.reading.data.api.BookService
import com.qianrenni.reading.data.api.ReadingProgressService
import com.qianrenni.reading.data.api.ShelfService
import com.qianrenni.reading.data.model.Book
import com.qianrenni.reading.data.model.ShelfItem
import com.qianrenni.reading.util.SnackBarManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class ShelfViewModel : ViewModel() {
    data class UiState(
        val shelfItems: List<ShelfItem> = emptyList(),
        val books: List<Book> = emptyList(),
        override val pageStatus: CommonPageStatus = CommonPageStatus(),
    ) : CommonUiState


    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadShelf() {
        if (uiState.value.pageStatus.isLoading) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(pageStatus = it.pageStatus.loading()) }

            val shelfItemsJob = async { ShelfService.getShelf() }
            val historyItemJob = async { ReadingProgressService.getReadingProgress() }
            val shelfItemsResult = shelfItemsJob.await()
            var shelfItems = emptyList<ShelfItem>()
            shelfItemsResult.onSuccess {
                shelfItems = it
            }
            val historyItemsResult = historyItemJob.await()
            historyItemsResult.onSuccess { historyItems ->
                val historyItemsMap = historyItems.associateBy { it.bookId }
                shelfItems = shelfItems.map { shelfItem ->
                    historyItemsMap[shelfItem.bookId]?.let {
                        return@map shelfItem.copy(
                            lastChapterId = it.lastChapterId,
                            lastPosition = it.lastPosition,
                            lastReadAt = it.lastReadAt
                        )
                    }
                    return@map shelfItem
                }
            }
            shelfItems.map { it.bookId }.let { bookIds ->
                if (bookIds.isNotEmpty()) {
                    BookService.getBooksByIds(bookIds).onSuccess { books ->
                        books.sortBy { it.id }
                        _uiState.update { it.copy(books = books.toList()) }
                    }
                }
            }
            val orders = shelfItems.indices.sortedByDescending { shelfItems[it].lastReadAt }
            _uiState.update {
                it.copy(
                    shelfItems = orders.map { index -> shelfItems[index] },
                    books = orders.map { index -> it.books[index] },
                    pageStatus = it.pageStatus.down()
                )
            }

        }
    }


    fun removeFromShelf(bookId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = ShelfService.removeFromShelf(bookId)
            result.onEmpty {
                SnackBarManager.showMessage("删除成功")
                _uiState.update { state ->
                    state.copy(
                        shelfItems = state.shelfItems.filter { it.bookId != bookId },
                        books = state.books.filter { it.id != bookId })
                }
            }
            result.onFailure { msg, _, _ ->
                SnackBarManager.showMessage(msg)
            }
        }
    }
}
