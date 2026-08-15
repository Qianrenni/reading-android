package com.qianrenni.reading.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qianrenni.reading.data.model.Catalog

@Composable
fun CatalogDrawer(
    modifier: Modifier = Modifier,
    isAscending: Boolean,
    state: LazyListState,
    catalog: List<Catalog>,
    currentChapterId: Int,
    onChapterSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    onReverseCatalog: () -> Unit
) {
    val sortedCatalog = remember(catalog, isAscending) {
        if (isAscending) catalog else catalog.reversed()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // 目录标题和排序按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "目录 (${catalog.size}章)",
                style = MaterialTheme.typography.titleMedium
            )

            TextButton(onClick = { onReverseCatalog() }) {
                Text(if (isAscending) "升序" else "降序")
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "切换排序",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 章节列表
        LazyColumn(
            modifier = Modifier
                .weight(1f),
            state = state,
            verticalArrangement = Arrangement.spacedBy(8.dp)

        ) {
            items(sortedCatalog, key = { it.id }) { item ->
                val isSelected = item.id == currentChapterId
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface
                        )
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onChapterSelected(item.id)
                                onDismiss()
                            },
                    )
                    Text(
                        text = "更新时间：${item.createdAt.replace("T", " ").replace("Z", " ")}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
