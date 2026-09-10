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
import com.qianrenni.reading.components.BookCoverImage
import com.qianrenni.reading.components.CommonPage
import com.qianrenni.reading.components.ContinueReadingButton
import com.qianrenni.reading.components.SwipeToDeleteItem
import com.qianrenni.reading.data.model.Book
import com.qianrenni.reading.data.model.ShelfItem
import com.qianrenni.reading.di.appContainer
import com.qianrenni.reading.navigation.BookRead
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.viewmodels.book.ShelfViewModel

@Composable
fun BookShelfView(
    navigator: Navigator,
    viewModel: ShelfViewModel = viewModel(factory = appContainer().viewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.loadShelf()
    }
    CommonPage(uiState, refresh = { viewModel.loadShelf() }, navigator = navigator) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                count = uiState.books.size,
                key = { uiState.books[it].id }
            ) { index ->
                ShelfItemCard(
                    shelfItem = uiState.shelfItems[index],
                    book = uiState.books[index],
                    onClick = { bookId, chapterId ->
                        navigator.navigate(BookRead(bookId = bookId, chapterId = chapterId))
                    },
                    onDelete = {
                        viewModel.removeFromShelf(it.bookId)
                    }
                )
            }
        }
    }
}

@Composable
fun ShelfItemCard(
    shelfItem: ShelfItem,
    book: Book,
    onClick: (Int, Int) -> Unit,
    onDelete: (ShelfItem) -> Unit
) {
    SwipeToDeleteItem(
        onDelete = { onDelete(shelfItem) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(book.id, shelfItem.lastChapterId ?: 0) }
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
                    shelfItem.lastReadAt?.let {
                        Text(
                            text = "上次阅读: ${it.split("T")[0]}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContinueReadingButton(onClick = { onClick(book.id, shelfItem.lastChapterId ?: 0) })
            }
        }
    }
}
