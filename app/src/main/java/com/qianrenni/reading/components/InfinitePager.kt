package com.qianrenni.reading.components

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun <T> InfiniteVerticalPager(
    items: List<T>,
    modifier: Modifier = Modifier,
    initialItemIndex: Int = 0,
    onPageChanged: ((Int) -> Unit)? = null,
    onBack: ((Int) -> Unit)? = null,
    onForward: ((Int) -> Unit)? = null,
    content: @Composable (item: T) -> Unit
) {
    if (items.isEmpty()) return

    val itemCount = items.size
    // 扩展总页数 = 原始数据 + 头部克隆1页 + 尾部克隆1页
    val extendedCount = itemCount + 2
    val realFirstIndex = 1

    val safeInitial = initialItemIndex.coerceIn(0, itemCount - 1)
    var lastPageIndex = realFirstIndex + safeInitial
    val pagerState = rememberPagerState(
        initialPage = lastPageIndex,
        pageCount = { extendedCount }
    )
    LaunchedEffect(pagerState, itemCount) {
        snapshotFlow {
            if (pagerState.isScrollInProgress) null
            else pagerState.currentPage
        }
            .filterNotNull()
            .collect { currentPage ->
                when (currentPage) {
                    0 -> {} // 克隆尾 -> 真实末页
                    extendedCount - 1 -> {}      // 克隆头 -> 真实首页
                    else -> {
                        if (lastPageIndex < currentPage) {
                            onForward?.invoke(currentPage - 1)
                        } else if (lastPageIndex > currentPage) {
                            onBack?.invoke(currentPage - 1)
                        }
                        onPageChanged?.invoke(currentPage - 1)
                        lastPageIndex = currentPage
                    }     // 中间页 -> 真实索引
                }

            }
    }
    // 监听滚动停止状态，到达克隆页时无动画瞬跳
    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
            .collect { (currentPage, isScrolling) ->
                // 仅在手指抬起/滚动停止时触发跳转，避免滑动过程中抖动
                if (!isScrolling) {
                    when (currentPage) {
                        0 -> pagerState.scrollToPage(itemCount)          // 虚拟前置页 -> 瞬跳到真实末页
                        extendedCount - 1 -> pagerState.scrollToPage(realFirstIndex) // 虚拟后置页 -> 瞬跳到真实首页
                    }
                }
            }
    }

    VerticalPager(
        state = pagerState,
        modifier = modifier,
    ) { pageIndex ->
        // 索引映射逻辑
        val item = when (pageIndex) {
            0 -> items[itemCount - 1]                // 头部克隆页 -> 真实最后一项
            extendedCount - 1 -> items[0]            // 尾部克隆页 -> 真实第一项
            else -> items[pageIndex - 1]             // 中间正常页 -> 真实对应项
        }
        content(item)
    }
}

@Composable
fun <T> InfiniteHorizontalPager(
    items: List<T>,
    modifier: Modifier = Modifier,
    initialItemIndex: Int = 0,
    onPageChanged: ((Int) -> Unit)? = null,
    onBack: ((Int) -> Unit)? = null,
    onForward: ((Int) -> Unit)? = null,
    content: @Composable (item: T) -> Unit
) {
    if (items.isEmpty()) return

    val itemCount = items.size
    // 扩展总页数 = 原始数据 + 头部克隆1页 + 尾部克隆1页
    val extendedCount = itemCount + 2
    val realFirstIndex = 1

    val safeInitial = initialItemIndex.coerceIn(0, itemCount - 1)
    var lastPageIndex = realFirstIndex + safeInitial
    val pagerState = rememberPagerState(
        initialPage = lastPageIndex,
        pageCount = { extendedCount }
    )
    LaunchedEffect(pagerState, itemCount) {
        snapshotFlow {
            if (pagerState.isScrollInProgress) null
            else pagerState.currentPage
        }
            .filterNotNull()
            .collect { currentPage ->
                Log.d("InfiniteHorizontalPager", "currentPage $currentPage ")
                when (currentPage) {
                    0 -> {} // 克隆尾 -> 真实末页
                    extendedCount - 1 -> {}      // 克隆头 -> 真实首页
                    else -> {
                        val diff = currentPage - lastPageIndex
                        when (diff) {
                            -1, itemCount - 1 -> {
                                onBack?.invoke(currentPage - 1)
                            }

                            1, -(itemCount - 1) -> {
                                onForward?.invoke(currentPage - 1)
                            }
                        }
                        onPageChanged?.invoke(currentPage - 1)
                        lastPageIndex = currentPage
                    }     // 中间页 -> 真实索引
                }

            }
    }
    // 监听滚动停止状态，到达克隆页时无动画瞬跳
    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
            .collect { (currentPage, isScrolling) ->
                // 仅在手指抬起/滚动停止时触发跳转，避免滑动过程中抖动
                if (!isScrolling) {
                    when (currentPage) {
                        0 -> pagerState.scrollToPage(itemCount)          // 虚拟前置页 -> 瞬跳到真实末页
                        extendedCount - 1 -> {
                            Log.d("InfiniteHorizontalPager", " scroll To end")
                            pagerState.scrollToPage(realFirstIndex)
                        } // 虚拟后置页 -> 瞬跳到真实首页
                    }
                }
            }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
    ) { pageIndex ->
        // 索引映射逻辑
        val item = when (pageIndex) {
            0 -> items[itemCount - 1]                // 头部克隆页 -> 真实最后一项
            extendedCount - 1 -> items[0]            // 尾部克隆页 -> 真实第一项
            else -> items[pageIndex - 1]             // 中间正常页 -> 真实对应项
        }
        content(item)
    }
}

@Preview
@Composable
fun BannerCarouselDemo() {
    val banners = listOf("首页", "发现", "我的", "设置")

    Column {
        InfiniteHorizontalPager(
            items = banners,
            modifier = Modifier
                .fillMaxWidth(),
            initialItemIndex = 0,
        ) { banner ->
            Text(banner, style = MaterialTheme.typography.headlineMedium)
        }
        InfiniteVerticalPager(
            items = banners,
            modifier = Modifier
                .fillMaxWidth(),
            initialItemIndex = 0,
        ) { banner ->
            Text(banner, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
