package com.qianrenni.reading.viewmodels.book

import com.qianrenni.reading.FakeBookApi
import com.qianrenni.reading.FakeCommentApi
import com.qianrenni.reading.FakeReadingProgressApi
import com.qianrenni.reading.FakeReportApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 覆盖 BookReadViewModel 的纯状态管理逻辑。
 * 说明：分页/翻页/心跳上报引擎涉及真实线程锁与无限循环，
 * 属于集成测试范畴（需 instrumentation），此处不覆盖。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookReadViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private fun newVm() = BookReadViewModel(
        FakeBookApi(),
        FakeCommentApi(),
        FakeReadingProgressApi(),
        FakeReportApi(),
        testDispatcher
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleCatalog toggles showCatalog and hides settings`() {
        val vm = newVm()
        assertFalse(vm.uiState.value.showCatalog)

        vm.toggleCatalog()
        assertTrue(vm.uiState.value.showCatalog)
        assertFalse(vm.uiState.value.showSettings)

        vm.toggleCatalog()
        assertFalse(vm.uiState.value.showCatalog)
    }

    @Test
    fun `toggleSettings toggles showSettings and hides catalog`() {
        val vm = newVm()
        assertFalse(vm.uiState.value.showSettings)

        vm.toggleSettings()
        assertTrue(vm.uiState.value.showSettings)
        assertFalse(vm.uiState.value.showCatalog)
    }

    @Test
    fun `hideAllDialogs resets overlays and shows system bars flag`() {
        val vm = newVm()
        vm.toggleCatalog()
        vm.toggleSettings()
        vm.toggleSystemBars()

        vm.hideAllDialogs()

        assertFalse(vm.uiState.value.showCatalog)
        assertFalse(vm.uiState.value.showSettings)
        assertFalse(vm.uiState.value.showBottomControls)
        assertTrue(vm.uiState.value.isSystemBarsHidden)
    }

    @Test
    fun `toggleSystemBars flips system bars and bottom controls`() {
        val vm = newVm()
        assertTrue(vm.uiState.value.isSystemBarsHidden)

        vm.toggleSystemBars()
        assertFalse(vm.uiState.value.isSystemBarsHidden)
        assertTrue(vm.uiState.value.showBottomControls)
    }

    @Test
    fun `clear resets pages state`() {
        val vm = newVm()
        vm.addPages(1, listOf(true), listOf(listOf("a")), listOf(0))

        vm.clear()

        assertTrue(vm.uiState.value.pages.isEmpty())
        assertEquals(0, vm.currentChapterPageIndex)
    }

    @Test
    fun `addPages stores into cache without sending`() {
        val vm = newVm()
        vm.addPages(1, listOf(true, false), listOf(listOf("a"), listOf("b")), listOf(0, 1))
        // 无界 channel：不消费也不会挂起
        assertTrue(vm.uiState.value.pages.isEmpty())
    }
}
