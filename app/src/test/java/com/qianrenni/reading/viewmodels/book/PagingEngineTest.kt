package com.qianrenni.reading.viewmodels.book

import com.qianrenni.reading.data.model.Catalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PagingEngineTest {

    private fun chapter(id: Int) = Catalog(id, "第${id}章", 100)

    private fun pages(count: Int) = (0 until count).map { i ->
        PageChapterItem(contents = listOf("p$i"), firstLineIndent = true, startLine = i)
    }

    // 3 章，每章 5 页
    private val catalog = listOf(chapter(101), chapter(102), chapter(103))
    private val cache: Map<Int, List<PageChapterItem>> = mapOf(
        101 to pages(5),
        102 to pages(5),
        103 to pages(5),
    )

    @Test
    fun `cannot compute with empty catalog`() {
        val outcome = PagingEngine.compute(emptyList(), emptyMap(), 0, 0, 0, 3, 1)
        assertTrue(outcome is PagingEngine.Outcome.CannotCompute)
    }

    @Test
    fun `cannot compute with invalid index`() {
        val outcome = PagingEngine.compute(catalog, cache, 5, 0, 0, 3, 1)
        assertTrue(outcome is PagingEngine.Outcome.CannotCompute)
    }

    @Test
    fun `needs loading when current chapter missing`() {
        val outcome = PagingEngine.compute(catalog, emptyMap(), 0, 0, 0, 3, 1)
        assertTrue(outcome is PagingEngine.Outcome.NeedsLoading)
        assertEquals(setOf(101), (outcome as PagingEngine.Outcome.NeedsLoading).missingChapterIds)
    }

    @Test
    fun `next page stays in chapter`() {
        val outcome = PagingEngine.compute(catalog, cache, 0, 0, 0, 3, 1)
        val ready = outcome as PagingEngine.Outcome.Ready
        assertEquals(0, ready.result.currentIndex)
        assertEquals(1, ready.result.currentChapterPageIndex)
        assertTrue(!ready.result.crossedChapter)
        assertEquals(101, ready.result.newChapterId)
        assertNull(ready.result.previousChapterId)
        assertEquals(3, ready.result.pages.size)
    }

    @Test
    fun `previous page stays in chapter`() {
        val outcome = PagingEngine.compute(catalog, cache, 0, 2, 0, 3, -1)
        val ready = outcome as PagingEngine.Outcome.Ready
        assertEquals(0, ready.result.currentIndex)
        assertEquals(1, ready.result.currentChapterPageIndex)
        assertTrue(!ready.result.crossedChapter)
    }

    @Test
    fun `cross to next chapter`() {
        val outcome = PagingEngine.compute(catalog, cache, 0, 4, 0, 3, 1)
        val ready = outcome as PagingEngine.Outcome.Ready
        assertEquals(1, ready.result.currentIndex)
        assertEquals(0, ready.result.currentChapterPageIndex)
        assertTrue(ready.result.crossedChapter)
        assertEquals(102, ready.result.newChapterId)
        assertEquals(101, ready.result.previousChapterId)
    }

    @Test
    fun `cross to previous chapter lands on last page`() {
        val outcome = PagingEngine.compute(catalog, cache, 1, 0, 0, 3, -1)
        val ready = outcome as PagingEngine.Outcome.Ready
        assertEquals(0, ready.result.currentIndex)
        assertEquals(4, ready.result.currentChapterPageIndex)
        assertTrue(ready.result.crossedChapter)
        assertEquals(101, ready.result.newChapterId)
        assertEquals(102, ready.result.previousChapterId)
    }

    @Test
    fun `wrap around from last to first chapter`() {
        val outcome = PagingEngine.compute(catalog, cache, 2, 4, 0, 3, 1)
        val ready = outcome as PagingEngine.Outcome.Ready
        assertEquals(0, ready.result.currentIndex)
        assertEquals(0, ready.result.currentChapterPageIndex)
        assertTrue(ready.result.crossedChapter)
    }

    @Test
    fun `wrap around from first to last chapter`() {
        val outcome = PagingEngine.compute(catalog, cache, 0, 0, 0, 3, -1)
        val ready = outcome as PagingEngine.Outcome.Ready
        assertEquals(2, ready.result.currentIndex)
        assertEquals(4, ready.result.currentChapterPageIndex)
        assertTrue(ready.result.crossedChapter)
    }

    @Test
    fun `needs loading for missing right neighbor on last page`() {
        val partialCache = mapOf(101 to pages(5))
        val outcome = PagingEngine.compute(catalog, partialCache, 0, 4, 0, 3, 0)
        assertTrue(outcome is PagingEngine.Outcome.NeedsLoading)
        assertTrue((outcome as PagingEngine.Outcome.NeedsLoading).missingChapterIds.contains(102))
    }

    @Test
    fun `needs loading for missing left neighbor on first page`() {
        val partialCache = mapOf(102 to pages(5), 103 to pages(5))
        val outcome = PagingEngine.compute(catalog, partialCache, 1, 0, 0, 3, 0)
        assertTrue(outcome is PagingEngine.Outcome.NeedsLoading)
        assertTrue((outcome as PagingEngine.Outcome.NeedsLoading).missingChapterIds.contains(101))
    }

    @Test
    fun `recompute keeps current page index and builds three pages`() {
        val outcome = PagingEngine.compute(catalog, cache, 0, 1, 2, 3, 0)
        val ready = outcome as PagingEngine.Outcome.Ready
        assertEquals(2, ready.result.currentPageIndex)
        assertEquals(3, ready.result.pages.size)
    }

    @Test
    fun `single chapter book wraps within itself`() {
        val single = listOf(chapter(1))
        val singleCache = mapOf(1 to pages(3))
        val outcome = PagingEngine.compute(single, singleCache, 0, 2, 0, 3, 1)
        val ready = outcome as PagingEngine.Outcome.Ready
        assertEquals(0, ready.result.currentIndex)
        assertEquals(0, ready.result.currentChapterPageIndex)
        assertEquals(3, ready.result.pages.size)
    }
}
