package com.qianrenni.reading.views

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qianrenni.reading.BookInfo
import com.qianrenni.reading.components.BookItem
import com.qianrenni.reading.data.model.Book
import com.qianrenni.reading.state.Navigator
import com.qianrenni.reading.viewmodels.book.HomeViewModel

@Composable
fun HomeView(
    navigator: Navigator,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gridState = rememberLazyStaggeredGridState()

    //  监听 scrollToTop 标志，切换分类时滚动到顶部
    LaunchedEffect(uiState.scrollToTop) {
        if (uiState.scrollToTop) {
            gridState.scrollToItem(0)
            viewModel.resetScrollFlag()
        }
    }

    //  监听滚动状态，自动加载更多（仅非搜索模式）
    LaunchedEffect(gridState, uiState.selectedCategory) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val totalItems = uiState.books.size
                if (lastVisibleIndex != null &&
                    lastVisibleIndex >= totalItems - 5 &&
                    !uiState.isLoading &&
                    uiState.selectedCategory.isNotEmpty()
                ) {
                    viewModel.loadMoreBooks()
                }
            }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 搜索输入框
            SearchBar(
                query = uiState.searchQuery,
                isSearching = uiState.isSearching,
                onQueryChanged = viewModel::onSearchQueryChanged,
                onClear = viewModel::clearSearch,
            )

            // 当搜索框为空时，显示分类选择栏和分类书籍
            if (uiState.searchQuery.isBlank()) {
                // 分类选择栏
                CategorySelector(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = viewModel::selectCategory
                )
                // 分类书籍网格
                BookGrid(
                    books = uiState.books,
                    gridState = gridState,
                    navigator = navigator,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // 搜索模式：显示搜索结果
                BookGrid(
                    books = uiState.searchResults,
                    gridState = gridState,
                    navigator = navigator,
                    isSearchResult = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    isSearching: Boolean,
    onQueryChanged: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("搜索书籍(书名或者作者名)") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索"
            )
        },
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(4.dp),
                    strokeWidth = 2.dp
                )
            } else if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清除搜索"
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun BookGrid(
    modifier: Modifier = Modifier,
    books: List<Book>,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    navigator: Navigator,
    isSearchResult: Boolean = false,
) {
    Box(modifier = modifier) {
        if (books.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSearchResult) "未找到相关书籍" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(160.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(books, key = { it.id }) { book ->
                    BookItem(
                        book = book,
                        onClick = {
                            navigator.navigate(BookInfo(bookId = book.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategorySelector(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .background(color = MaterialTheme.colorScheme.surface),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        categories.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}