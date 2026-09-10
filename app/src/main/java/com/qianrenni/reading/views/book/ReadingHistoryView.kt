package com.qianrenni.reading.views.book

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qianrenni.reading.components.AddToShelfButton
import com.qianrenni.reading.components.BookCoverImage
import com.qianrenni.reading.components.CommonPage
import com.qianrenni.reading.components.ContinueReadingButton
import com.qianrenni.reading.components.SwipeToDeleteItem
import com.qianrenni.reading.data.model.Book
import com.qianrenni.reading.data.model.BookReadingProgress
import com.qianrenni.reading.di.appContainer
import com.qianrenni.reading.navigation.BookRead
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.viewmodels.book.HistoryViewModel

@Composable
fun ReadingHistoryView(
    navigator: Navigator,
    viewModel: HistoryViewModel = viewModel(factory = appContainer().viewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }
    CommonPage(
        uiState = uiState,
        refresh = { viewModel.loadHistory() },
        navigator = navigator
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.books.size, key = { uiState.books[it].id }) { item ->
                HistoryItemCard(
                    historyItem = uiState.historyItems[item],
                    book = uiState.books[item],
                    isInShelf = uiState.shelfIds.contains(uiState.books[item].id),
                    onClick = { bookId, chapterId ->
                        navigator.navigate(BookRead(bookId = bookId, chapterId = chapterId))
                    },
                    onDelete = {
                        viewModel.deleteHistory(it.bookId)
                    },
                    onAddToShelf = {
                        viewModel.addToShelf(it.id)
                    }
                )
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    historyItem: BookReadingProgress,
    book: Book,
    isInShelf: Boolean,
    onClick: (Int, Int) -> Unit,
    onDelete: (BookReadingProgress) -> Unit,
    onAddToShelf: (Book) -> Unit
) {
    SwipeToDeleteItem(
        onDelete = { onDelete(historyItem) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(book.id, historyItem.lastChapterId) }
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BookCoverImage(book = book)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = book.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "上次阅读: ${historyItem.lastReadAt.split("T")[0]}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isInShelf) {
                    AddToShelfButton(onClick = { onAddToShelf(book) })
                }
                ContinueReadingButton(onClick = { onClick(book.id, historyItem.lastChapterId) })
            }
        }
    }
}
