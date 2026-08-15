package com.qianrenni.reading.viewmodels.book

import android.app.Application
import android.util.Log
import androidx.collection.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qianrenni.reading.common.CommonPageStatus
import com.qianrenni.reading.common.CommonUiState
import com.qianrenni.reading.data.api.BookService
import com.qianrenni.reading.data.api.ReadingProgressService
import com.qianrenni.reading.data.api.ReportService
import com.qianrenni.reading.data.model.Book
import com.qianrenni.reading.data.model.Catalog
import com.qianrenni.reading.data.model.ReadEvent
import com.qianrenni.reading.data.model.UpdateProgressRequest
import com.qianrenni.reading.util.indexToCN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.internal.notifyAll
import okhttp3.internal.wait
import kotlin.time.Duration.Companion.seconds

data class BookChapter(val chapterId: Int, val chapterContent: String)
data class PageChapterItem(
    val contents: List<String>, val firstLineIndent: Boolean, val endClip: Boolean = true
)

data class BookReadUiState(
    val book: Book? = null,
    val catalog: List<Catalog> = emptyList(),
    val pages: List<PageChapterItem> = emptyList(),
    val showCatalog: Boolean = false,
    val showSettings: Boolean = false,
    val showBottomControls: Boolean = false,
    val currentIndex: Int = -1,
    val currentPageIndex: Int = 0,
    val isSystemBarsHidden: Boolean = true,
    override val pageStatus: CommonPageStatus = CommonPageStatus()
) : CommonUiState

private const val TAG = "BookReadViewModel"

class BookReadViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BookReadUiState())
    val uiState: StateFlow<BookReadUiState> = _uiState.asStateFlow()
    private val chaptersCache = LruCache<Int, List<PageChapterItem>>(5)
    val bookChapterChannel = Channel<BookChapter>()
    val lock = Any()
    private val pageSize = 3
    private var heartbeatJob: Job? = null
    var currentChapterPageIndex: Int = 0

    private var restartJob: Job? = null
    fun clear() {
        synchronized(lock) {
            chaptersCache.evictAll()
            currentChapterPageIndex = 0
            _uiState.update { it.copy(pages = emptyList()) }
        }
    }

    fun catalogIndexToLoad(index: Int) {
        val catalog = uiState.value.catalog
        listOf(
            (index - 1 + catalog.size) % catalog.size,
            index,
            (index + 1) % catalog.size
        ).forEach {
            loadChapter(catalog[it].id)
        }
    }

    private fun lockForChapter(chapterId: Int) {
        synchronized(lock) {
            while (chaptersCache[chapterId] == null) {
                _uiState.update { it.copy(pageStatus = it.pageStatus.loading()) }
                Log.d(TAG, "lockForChapter: $chapterId")
                lock.wait()
                Log.d(TAG, "lockForChapter: retry $chapterId")
            }
        }
    }

    fun refreshPages(step: Int = 0, currentPage: Int = uiState.value.currentPageIndex) {
        Log.d(TAG, "refreshPages:  currentPage $currentPage")
        viewModelScope.launch(Dispatchers.Default) {
            val catalog = uiState.value.catalog
            var updateCurrentIndex = uiState.value.currentIndex
            currentChapterPageIndex += step
            var currentChapterId = catalog[updateCurrentIndex].id
            lockForChapter(currentChapterId)
            var items = chaptersCache[currentChapterId]!!
            if (currentChapterPageIndex == -1) {
                stopHeartbeat()
                reportChapterRead(catalog[updateCurrentIndex].id, "exit")
                updateCurrentIndex = (updateCurrentIndex - 1 + catalog.size) % catalog.size
                currentChapterId = catalog[updateCurrentIndex].id
                lockForChapter(currentChapterId)
                currentChapterPageIndex = chaptersCache[currentChapterId]!!.size - 1
                uiState.value.book?.let {
                    ReadingProgressService.updateReadingProgress(
                        UpdateProgressRequest(
                            it.id, currentChapterId
                        )
                    )
                }
                reportChapterRead(currentChapterId, "enter")
                startHeartbeat(currentChapterId)
            } else if (currentChapterPageIndex == items.size) {
                stopHeartbeat()
                reportChapterRead(catalog[updateCurrentIndex].id, "exit")
                updateCurrentIndex = (updateCurrentIndex + 1 + catalog.size) % catalog.size
                currentChapterId = catalog[updateCurrentIndex].id
                lockForChapter(currentChapterId)
                currentChapterPageIndex = 0
                uiState.value.book?.let {
                    ReadingProgressService.updateReadingProgress(
                        UpdateProgressRequest(
                            it.id, currentChapterId
                        )
                    )
                }
                reportChapterRead(currentChapterId, "enter")
                startHeartbeat(currentChapterId)

            }
            catalogIndexToLoad(updateCurrentIndex)
            items = chaptersCache[currentChapterId]!!
            var targetIndex = currentChapterPageIndex
            if (items.size - currentChapterPageIndex <= 1) {
                val rightChapterId = catalog[(updateCurrentIndex + 1) % catalog.size].id
                lockForChapter(rightChapterId)
                items = items + chaptersCache[rightChapterId]!!
            }
            if (currentChapterPageIndex < 1) {
                val leftChapterId =
                    catalog[(updateCurrentIndex - 1 + catalog.size) % catalog.size].id
                lockForChapter(leftChapterId)
                items = chaptersCache[leftChapterId]!! + items
                targetIndex += chaptersCache[leftChapterId]!!.size
            }
            val updateItems =
                listOf(targetIndex, targetIndex - 1, targetIndex + 1).map { items[it] }
            val pagesOrder = listOf(
                currentPage,
                (currentPage - 1 + pageSize) % pageSize,
                (currentPage + 1) % pageSize
            )
            _uiState.update { state ->
                state.copy(
                    pages = pagesOrder.zip(updateItems).sortedBy { it.first }
                        .map { it.second },
                    currentIndex = updateCurrentIndex,
                    pageStatus = state.pageStatus.down(),
                    currentPageIndex = currentPage
                )
            }
            Log.d(TAG, "refreshPages: ${uiState.value.pages}")
        }

    }

    fun addPages(chapterId: Int, indents: List<Boolean>, contents: List<List<String>>) {
        val pageChapterItem = contents.mapIndexed { index, strings ->
            PageChapterItem(
                firstLineIndent = indents[index],
                contents = strings,
                endClip = if (index < contents.size - 1) {
                    !indents[index + 1]
                } else {
                    false
                }
            )
        }
        synchronized(lock) {
            chaptersCache.put(chapterId, pageChapterItem)
            lock.notifyAll()
        }
    }

    fun loadBookAndCatalog(bookId: Int, initialChapterId: Int) {
        require(bookId > 0)
        require(initialChapterId > 0)
        val currentState = _uiState.value
        if (currentState.pageStatus.isLoading) {
            return
        }
        _uiState.update { it.copy(pageStatus = it.pageStatus.loading()) }
        clear()
        viewModelScope.launch(Dispatchers.IO) {
            val bookJob = async { BookService.getBookById(bookId) }
            val catalogJob = async { BookService.getCatalog(bookId) }
            val bookResult = bookJob.await()
            val catalogResult = catalogJob.await()
            bookResult.onSuccess { data ->
                _uiState.update { it.copy(book = data) }
            }
            bookResult.onFailure { text, _, _ ->
                _uiState.update { it.copy(pageStatus = it.pageStatus.error(text)) }
            }
            catalogResult.onSuccess { data ->
                val catalogList = data.toList()
                    .mapIndexed { index, it -> it.copy(title = "第${indexToCN(index + 1)}章 ${it.title}") }
                _uiState.update { it.copy(catalog = catalogList) }
                val chapterIdToLoad = if (initialChapterId > 0) {
                    initialChapterId
                } else if (catalogList.isNotEmpty()) {
                    catalogList.first().id
                } else {
                    -1
                }
                if (chapterIdToLoad > 0) {
                    // 加载章节内容
                    _uiState.update {
                        it.copy(
                            currentIndex = it.catalog.indexOfFirst { item -> item.id == chapterIdToLoad },
                        )
                    }
                    catalogIndexToLoad(uiState.value.currentIndex)
                    ReadingProgressService.updateReadingProgress(
                        UpdateProgressRequest(
                            bookId, chapterIdToLoad
                        )
                    )
                    reportChapterRead(chapterIdToLoad, "enter")
                    startHeartbeat(chapterIdToLoad)
                    refreshPages()
                }
            }
            catalogResult.onFailure { text, _, _ ->
                _uiState.update { it.copy(pageStatus = it.pageStatus.error(text)) }
            }
        }
    }

    private fun loadChapter(chapterId: Int) {
        chaptersCache[chapterId]?.let {
            Log.d(TAG, "loadChapter: Cached $chapterId")
            return
        }
        val bookId = _uiState.value.book?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // 获取章节内容
            val result = BookService.getChapter(chapterId, bookId)
            result.onSuccess { data ->
                bookChapterChannel.send(
                    BookChapter(
                        chapterId = chapterId, chapterContent = data
                    )
                )
            }
            result.onFailure { text, _, _ ->
                _uiState.update {
                    it.copy(
                        pageStatus = it.pageStatus.error(
                            text
                        )
                    )
                }
            }
        }
    }

    fun goChapter(step: Int) {
        require(step == 1 || step == -1)
        val currentIndex = uiState.value.currentIndex
        val catalog = uiState.value.catalog
        val updateCurrentIndex = (currentIndex + step + catalog.size) % catalog.size
        loadChapter(catalog[updateCurrentIndex].id)
        _uiState.update { it.copy(currentIndex = updateCurrentIndex) }
        currentChapterPageIndex = 0
        viewModelScope.launch(Dispatchers.IO) {
            ReadingProgressService.updateReadingProgress(
                UpdateProgressRequest(
                    bookId = uiState.value.book?.id ?: -1,
                    lastChapterId = catalog[updateCurrentIndex].id
                )
            )
        }
        refreshPages()
    }

    fun goChapterId(chapterId: Int) {
        val targetIndex = uiState.value.catalog.indexOfFirst { it.id == chapterId }
        loadChapter(chapterId)
        _uiState.update { it.copy(currentIndex = targetIndex) }
        currentChapterPageIndex = 0
        viewModelScope.launch(Dispatchers.IO) {
            ReadingProgressService.updateReadingProgress(
                UpdateProgressRequest(
                    bookId = uiState.value.book?.id ?: -1,
                    lastChapterId = chapterId
                )
            )
        }
        refreshPages()
    }

    fun toggleCatalog() {
        _uiState.update {
            it.copy(
                showSettings = false,
                showCatalog = !it.showCatalog,
            )
        }
    }

    fun toggleSettings() {
        _uiState.update {
            it.copy(
                showSettings = !it.showSettings,
                showCatalog = false,
            )
        }
    }


    fun hideAllDialogs() {
        _uiState.update {
            it.copy(
                showCatalog = false,
                showSettings = false,
                showBottomControls = false,
                isSystemBarsHidden = true,
            )
        }
    }

    fun toggleSystemBars() {
        _uiState.update {
            it.copy(
                isSystemBarsHidden = !it.isSystemBarsHidden,
                showBottomControls = !it.showBottomControls,
                showCatalog = false,
                showSettings = false
            )
        }
    }


    private fun reportChapterRead(chapterId: Int, eventType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            uiState.value.book?.let { book ->
                ReportService.reportChapterRead(
                    ReadEvent(
                        bookId = book.id, chapterId = chapterId, eventType = eventType
                    )
                )
            }
        }
    }

    private fun startHeartbeat(chapterId: Int) {
        stopHeartbeat()
        heartbeatJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(10000) // 每10秒上报一次
                reportChapterRead(chapterId, "heartbeat")
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun restart() {
        restartJob?.cancel()
        restartJob = viewModelScope.launch {
            delay(1.seconds)
            clear()
            catalogIndexToLoad(uiState.value.currentIndex)
            refreshPages()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 上报离开当前章节
        if (uiState.value.currentIndex > 0) {
            reportChapterRead(uiState.value.catalog[uiState.value.currentIndex].id, "exit")
        }
        stopHeartbeat()
        clear()
    }
}
