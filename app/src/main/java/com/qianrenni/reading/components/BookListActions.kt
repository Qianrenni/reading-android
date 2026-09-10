package com.qianrenni.reading.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.qianrenni.reading.R
import com.qianrenni.reading.data.model.Book

/** 列表项封面默认尺寸与圆角。 */
private val COVER_WIDTH = 90.dp
private val COVER_HEIGHT = 120.dp
private val COVER_SHAPE = RoundedCornerShape(8.dp)

/** 列表项操作按钮统一高度与圆角。 */
private val ACTION_HEIGHT = 34.dp
private val ACTION_SHAPE = RoundedCornerShape(17.dp)

/**
 * 书籍封面：统一占位图、错误图与圆角。
 *
 * @param width / height 传 null 表示尺寸完全由 [modifier] 决定（如 `weight(1f)`、`fillMaxWidth()`）
 */
@Composable
fun BookCoverImage(
    book: Book,
    modifier: Modifier = Modifier,
    width: Dp? = COVER_WIDTH,
    height: Dp? = COVER_HEIGHT,
    shape: Shape = COVER_SHAPE,
    contentScale: ContentScale = ContentScale.Crop
) {
    Image(
        painter = rememberAsyncImagePainter(
            model = book.cover,
            placeholder = painterResource(R.drawable.skeleton),
            error = painterResource(R.drawable.skeleton)
        ),
        contentDescription = book.name,
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .then(if (height != null) Modifier.height(height) else Modifier)
            .clip(shape),
        contentScale = contentScale
    )
}

/** 主操作按钮：继续阅读。 */
@Composable
fun ContinueReadingButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(ACTION_HEIGHT),
        shape = ACTION_SHAPE,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    ) {
        Text(text = "继续阅读", style = MaterialTheme.typography.labelLarge)
    }
}

/** 次操作按钮：加入书架。 */
@Composable
fun AddToShelfButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(ACTION_HEIGHT),
        shape = ACTION_SHAPE,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    ) {
        Text(text = "加入书架", style = MaterialTheme.typography.labelLarge)
    }
}
