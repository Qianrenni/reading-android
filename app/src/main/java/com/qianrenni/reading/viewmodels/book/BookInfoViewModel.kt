package com.qianrenni.reading.viewmodels.book

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qianrenni.reading.common.CommonPageStatus
import com.qianrenni.reading.common.CommonUiState
import com.qianrenni.reading.data.api.BookService
import com.qianrenni.reading.data.api.CommentService
import com.qianrenni.reading.data.api.NetworkResult
import com.qianrenni.reading.data.model.Book
import com.qianrenni.reading.data.model.BookComment
import com.qianrenni.reading.data.model.Catalog
import com.qianrenni.reading.util.SnackBarManager
import com.qianrenni.reading.util.indexToCN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val book: Book? = null,
    val catalog: List<Catalog> = emptyList(),
    val relatedBooks: List<Book> = emptyList(),
    val selectedTabIndex: Int = 0,
    // ===== 书评 =====
    val reviews: List<BookComment> = emptyList(),
    val reviewTotal: Int = 0,
    val reviewPage: Int = 1,
    val reviewSize: Int = 5,
    val myReview: BookComment? = null,
    val reviewsLoading: Boolean = false,
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

    // ==================== 书评 ====================

    /** 分页加载书评列表 */
    fun loadReviews(bookId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            _uiState.update { it.copy(reviewsLoading = true) }
            val result = CommentService.getBookReviews(bookId, state.reviewPage, state.reviewSize)
            result.onSuccess { page ->
                _uiState.update {
                    it.copy(
                        reviews = page.items,
                        reviewTotal = page.total,
                        reviewsLoading = false
                    )
                }
            }
            result.onFailure { message, _, _ ->
                _uiState.update { it.copy(reviewsLoading = false) }
                SnackBarManager.showMessage(message)
            }
        }
    }

    /** 加载自己的书评（无则置 null） */
    fun loadMyReview(bookId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = CommentService.getMyBookReview(bookId)
            when (result) {
                is NetworkResult.Success -> _uiState.update { it.copy(myReview = result.data) }
                is NetworkResult.Empty -> _uiState.update { it.copy(myReview = null) }
                is NetworkResult.Failure -> Unit
            }
        }
    }

    /** 翻页 */
    fun setReviewPage(page: Int) {
        val current = _uiState.value
        if (page == current.reviewPage) return
        _uiState.update { it.copy(reviewPage = page) }
        current.book?.id?.let { loadReviews(it) }
    }

    /** 发布/编辑自己的书评 */
    fun createReview(bookId: Int, content: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmed = content.trim()
            if (trimmed.isEmpty()) {
                SnackBarManager.showMessage("评论内容不能为空")
                return@launch
            }
            if (trimmed.length > 300) {
                SnackBarManager.showMessage("评论内容不能超过300字")
                return@launch
            }
            val result = CommentService.createBookReview(bookId, trimmed)
            if (result is NetworkResult.Success || result is NetworkResult.Empty) {
                SnackBarManager.showMessage("书评发布成功")
                loadReviews(bookId)
                loadMyReview(bookId)
                withContext(Dispatchers.Main) { onSuccess() }
            } else {
                SnackBarManager.showMessage((result as? NetworkResult.Failure)?.message ?: "发布失败")
            }
        }
    }

    /** 删除自己的书评 */
    fun deleteReview(bookId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = CommentService.deleteBookReview(bookId)
            if (result is NetworkResult.Success || result is NetworkResult.Empty) {
                SnackBarManager.showMessage("书评已删除")
                loadReviews(bookId)
                loadMyReview(bookId)
                withContext(Dispatchers.Main) { onSuccess() }
            } else {
                SnackBarManager.showMessage((result as? NetworkResult.Failure)?.message ?: "删除失败")
            }
        }
    }
}
