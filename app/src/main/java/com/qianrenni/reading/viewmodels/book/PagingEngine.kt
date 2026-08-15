package com.qianrenni.reading.viewmodels.book

import com.qianrenni.reading.data.model.Catalog

/**
 * 分页计算的结果。
 */
data class PagingResult(
    val currentIndex: Int,
    val currentChapterPageIndex: Int,
    val pages: List<PageChapterItem>,
    val currentPageIndex: Int,
    /** 本次是否跨越到相邻章节 */
    val crossedChapter: Boolean,
    val newChapterId: Int,
    /** 跨越时离开的章节 id，未跨章为 null */
    val previousChapterId: Int?,
)

/**
 * 阅读器分页引擎：纯函数，无 IO / 锁 / 协程，便于彻底单元测试。
 *
 * 职责：给定目录、已加载的章节分页缓存、当前阅读位置与翻页意图，
 * 计算下一页应展示的三页窗口、目标章节下标及是否跨章。
 */
object PagingEngine {

    sealed interface Outcome {
        /** 计算成功 */
        data class Ready(val result: PagingResult) : Outcome

        /** 缺少章节数据，需要先加载 [missingChapterIds] 后再试 */
        data class NeedsLoading(val missingChapterIds: Set<Int>) : Outcome

        /** 无法计算（目录为空或当前位置非法） */
        object CannotCompute : Outcome
    }

    /**
     * 计算翻页后的展示状态。
     *
     * @param catalog 目录（按阅读顺序）
     * @param pagesByChapter 已加载章节的分页数据（chapterId -> 页面列表）
     * @param currentIndex 当前章节在目录中的下标
     * @param currentChapterPageIndex 当前章内页下标
     * @param currentPage 当前屏幕页下标（0..pageCount-1）
     * @param pageCount 单屏展示页数
     * @param step 翻页步长：-1 上一页、0 重算、1 下一页
     */
    fun compute(
        catalog: List<Catalog>,
        pagesByChapter: Map<Int, List<PageChapterItem>>,
        currentIndex: Int,
        currentChapterPageIndex: Int,
        currentPage: Int,
        pageCount: Int,
        step: Int,
    ): Outcome {
        if (catalog.isEmpty() || currentIndex !in catalog.indices) return Outcome.CannotCompute

        val startId = catalog[currentIndex].id
        val startItems = pagesByChapter[startId]
        if (startItems.isNullOrEmpty()) return Outcome.NeedsLoading(setOf(startId))

        var effIndex = currentIndex
        var effPage = currentChapterPageIndex + step

        // 跨章：翻到上一章末页 / 下一章首页
        if (effPage == -1) {
            effIndex = (effIndex - 1 + catalog.size) % catalog.size
            val items = pagesByChapter[catalog[effIndex].id]
            if (items.isNullOrEmpty()) return Outcome.NeedsLoading(setOf(catalog[effIndex].id))
            effPage = items.size - 1
        } else if (effPage == startItems.size) {
            effIndex = (effIndex + 1 + catalog.size) % catalog.size
            val items = pagesByChapter[catalog[effIndex].id]
            if (items.isNullOrEmpty()) return Outcome.NeedsLoading(setOf(catalog[effIndex].id))
            effPage = 0
        }

        val effItems = pagesByChapter.getValue(catalog[effIndex].id)
        val rightId = catalog[(effIndex + 1) % catalog.size].id
        val leftId = catalog[(effIndex - 1 + catalog.size) % catalog.size].id

        // 需要左/右邻章来补足三页窗口
        val missing = mutableSetOf<Int>()
        if (effItems.size - effPage <= 1 && pagesByChapter[rightId].isNullOrEmpty()) missing += rightId
        if (effPage < 1 && pagesByChapter[leftId].isNullOrEmpty()) missing += leftId
        if (missing.isNotEmpty()) return Outcome.NeedsLoading(missing)

        var items = effItems
        var targetIndex = effPage
        if (effItems.size - effPage <= 1) {
            items = items + pagesByChapter.getValue(rightId)
        }
        if (effPage < 1) {
            items = pagesByChapter.getValue(leftId) + items
            targetIndex += pagesByChapter.getValue(leftId).size
        }
        val updateItems = listOf(targetIndex, targetIndex - 1, targetIndex + 1).map { items[it] }
        val pagesOrder = listOf(
            currentPage,
            (currentPage - 1 + pageCount) % pageCount,
            (currentPage + 1) % pageCount
        )
        val pages = pagesOrder.zip(updateItems).sortedBy { it.first }.map { it.second }

        return Outcome.Ready(
            PagingResult(
                currentIndex = effIndex,
                currentChapterPageIndex = effPage,
                pages = pages,
                currentPageIndex = currentPage,
                crossedChapter = effIndex != currentIndex,
                newChapterId = catalog[effIndex].id,
                previousChapterId = if (effIndex != currentIndex) catalog[currentIndex].id else null,
            )
        )
    }
}
