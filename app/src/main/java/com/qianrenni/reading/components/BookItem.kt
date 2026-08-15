package com.qianrenni.reading.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.qianrenni.reading.R
import com.qianrenni.reading.data.model.Book

@Composable
fun BookItem(
    book: Book,
    modifier: Modifier = Modifier,
    onClick: (Book) -> Unit = {},
) {
    Column(
        modifier = modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .clickable(
                onClick = {
                    onClick(book)
                }
            )
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = book.cover,
                // 可选：添加占位/错误状态
                placeholder = painterResource(R.drawable.skeleton),
                error = painterResource(R.drawable.skeleton)
            ),
            contentDescription = book.name,
            modifier = Modifier
                .fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )
        // 书籍信息
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = book.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Right,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = book.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
