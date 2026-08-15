package com.qianrenni.reading.views.book

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.qianrenni.reading.BookInfo
import com.qianrenni.reading.BookRead
import com.qianrenni.reading.components.BookItem
import com.qianrenni.reading.components.CommonPage
import com.qianrenni.reading.data.model.Book
import com.qianrenni.reading.data.model.Catalog
import com.qianrenni.reading.state.Navigator
import com.qianrenni.reading.viewmodels.book.BookInfoViewModel

@Composable
fun BookInfoView(
    navigator: Navigator,
    bookId: Int,
    viewModel: BookInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(bookId) {
        viewModel.loadBookInfo(bookId)
    }
    CommonPage(
        uiState = uiState,
        refresh = { viewModel.loadBookInfo(bookId) },
        navigator = navigator
    )
    {
        uiState.book?.let { book ->
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // 书籍信息卡片
                item {
                    BookInfoCard(book = book)
                }

                // Tab 选择器
                item {
                    BookInfoTabs(
                        selectedTabIndex = uiState.selectedTabIndex,
                        onTabSelected = viewModel::selectTab
                    )
                }

                // Tab 内容
                item {
                    when (uiState.selectedTabIndex) {
                        0 -> {
                            Text(
                                text = book.description,
                                modifier = Modifier.padding(4.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        1 -> {
                            // 目录
                            CatalogList(
                                catalog = uiState.catalog,
                                bookId = book.id,
                                navigator = navigator
                            )
                        }
                    }
                }

                // 相关推荐
                item {
                    RelatedBooksSection(
                        relatedBooks = uiState.relatedBooks,
                        onBookClick = { clickedBook ->
                            navigator.navigate(BookInfo(bookId = clickedBook.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookInfoCard(book: Book) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 封面图片
        AsyncImage(
            model = book.cover,
            contentDescription = book.name,
            modifier = Modifier.weight(1f),
            contentScale = ContentScale.FillWidth,
        )
        // 书籍信息
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = book.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "作者",
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "创建日期",
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = book.createdAt.split('T').firstOrNull() ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // 标签
            if (book.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    book.tags.split(",").forEach { tag ->
                        Text(
                            text = tag.trim(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 统计信息
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "章节数",
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${book.totalChapter} 章节",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun BookInfoTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("书籍简介", "目录")

    SecondaryTabRow(
        selectedTabIndex,
        Modifier.fillMaxWidth(),
        TabRowDefaults.primaryContainerColor,
        TabRowDefaults.primaryContentColor
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = { Text(text = title) }
            )
        }
    }
}

@Composable
private fun CatalogList(
    catalog: List<Catalog>,
    bookId: Int,
    navigator: Navigator
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        items(catalog, key = { it.id }) { item ->
            Text(
                text = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navigator.navigate(BookRead(bookId = bookId, chapterId = item.id))
                    }
                    .padding(8.dp),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

        }
    }
}

@Composable
private fun RelatedBooksSection(
    relatedBooks: List<Book>,
    onBookClick: (Book) -> Unit
) {
    if (relatedBooks.isEmpty()) return

    Column {
        Text(
            text = "相关推荐",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.Start, // 重要：左对齐
            modifier = Modifier.fillMaxWidth()
        ) {
            relatedBooks.forEachIndexed { index, book ->
                val isLastAndOdd = (index == relatedBooks.lastIndex) && (relatedBooks.size % 2 != 0)
                Box(
                    modifier = Modifier
                        // 如果是落单的最后一个，强制宽度为 50%
                        // 否则，使用 weight(1f) 让两个元素平分
                        .then(
                            if (isLastAndOdd) {
                                Modifier.fillMaxWidth(0.5f)
                            } else {
                                Modifier.weight(1f)
                            }
                        )
                ) {
                    BookItem(
                        book = book,
                        onClick = onBookClick
                    )
                }
            }
        }
    }
}
