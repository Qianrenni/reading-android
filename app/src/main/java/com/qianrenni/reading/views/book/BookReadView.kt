package com.qianrenni.reading.views.book

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.qianrenni.reading.BookInfo
import com.qianrenni.reading.R
import com.qianrenni.reading.components.BottomControlBar
import com.qianrenni.reading.components.CatalogDrawer
import com.qianrenni.reading.components.CommonPage
import com.qianrenni.reading.components.InfiniteHorizontalPager
import com.qianrenni.reading.components.ReadingSettings
import com.qianrenni.reading.data.model.ReadSettings
import com.qianrenni.reading.data.store.AuthStore
import com.qianrenni.reading.data.store.SettingsRepository
import com.qianrenni.reading.state.Navigator
import com.qianrenni.reading.util.SystemBarUtils
import com.qianrenni.reading.viewmodels.book.BookReadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "BOOK READ VIEW"
suspend fun measureText(
    content: String,
    density: Density,
    heightPx: Float,
    widthPx: Float,
    textMeasurer: TextMeasurer,
    readSettings: ReadSettings,
    onCallBack: (List<Boolean>, List<List<String>>, List<Int>) -> Unit
) {
    withContext(Dispatchers.Default) {
        val newPages = mutableListOf<List<String>>()
        // 每页首段在整个章节中的行号（用于行评论定位）
        val startLines = mutableListOf<Int>()
        var lineCount = 0
        var startIndex = 0
        val processedContent =
            content.split(Regex("\\s+"))
                .filter { it.trim().isNotBlank() }
                .joinToString(separator = "\n")
        val totalLength = processedContent.length
        val paddingPx = with(density) { (2 * readSettings.fontSize).dp.toPx() }
        val tempIsIndent = MutableList(1) { true }
        while (startIndex < totalLength) {
            // 初始猜测：从当前索引开始，向后移动约一页的字符数
            var low = startIndex
            var high = totalLength
            var bestFitIndex = low
            var textToMeasure: List<String>?
            var best: List<String> = emptyList()
            // 二分查找当前页能容纳的最大字符索引
            while (low <= high) {
                val mid = (low + high) / 2
                textToMeasure =
                    processedContent.substring(startIndex, mid)
                        .split("\n")
                        .filter { it.isNotBlank() }
                val sumHeight = textToMeasure.mapIndexed { index, it ->
                    textMeasurer.measure(
                        text = if (
                            index == textToMeasure.size - 1
                            && processedContent[mid.coerceIn(0, mid - 1)] != '\n'
                        ) {
                            it
                        } else {
                            "${it}中"
                        },
                        style = TextStyle(
                            fontSize = readSettings.fontSize.sp,
                            lineHeight = readSettings.lineHeight.sp,
                            letterSpacing = readSettings.letterSpacing.sp,
                            fontFamily = readSettings.fontFamily,
                            textIndent = if (index == 0) {
                                if (tempIsIndent.last()) {
                                    TextIndent(firstLine = (readSettings.fontSize * 2).sp)
                                } else {
                                    null
                                }
                            } else {
                                TextIndent(firstLine = (readSettings.fontSize * 2).sp)
                            },
                            textAlign = TextAlign.Justify
                        ),
                        constraints = Constraints(
                            maxWidth = widthPx.toInt(),
                            maxHeight = Int.MAX_VALUE
                        )
                    )
                }.sumOf { it.size.height }
                // 使用 TextMeasurer 进行精确测量
                if ((sumHeight + (textToMeasure.size - 1) * paddingPx) <= heightPx) {
                    bestFitIndex = mid
                    low = mid + 1
                    best = textToMeasure
                } else {
                    high = mid - 1
                }
            }
            best.let {
                if (it.isNotEmpty()) {
                    newPages.add(best)
                    startLines.add(lineCount)
                    lineCount += best.size
                    tempIsIndent.add(
                        processedContent[bestFitIndex.coerceIn(
                            0,
                            bestFitIndex - 1
                        )] == '\n'
                    )
                }  // 避免添加空页
            }
            startIndex = bestFitIndex
        }
        onCallBack(tempIsIndent.toList(), newPages, startLines)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReadView(
    context: Context,
    navigator: Navigator,
    bookId: Int,
    chapterId: Int,
    viewModel: BookReadViewModel = viewModel()
) {

    val activity = LocalActivity.current as Activity
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadBookAndCatalog(bookId, chapterId)
    }
    // 控制系统栏显示/隐藏
    LaunchedEffect(uiState.isSystemBarsHidden) {
        if (uiState.isSystemBarsHidden) {
            SystemBarUtils.hideSystemBars(activity)
        } else {
            SystemBarUtils.showSystemBars(activity)
        }
    }

    var isAscending by remember { mutableStateOf(true) }
    val lazyListState = rememberLazyListState()
    // 行评论弹层状态
    var selectedCommentLine by remember { mutableStateOf<Int?>(null) }
    var selectedLineText by remember { mutableStateOf("") }
    var commentInput by remember { mutableStateOf("") }
    val colorScheme = MaterialTheme.colorScheme
    val settingsRepository =
        remember { SettingsRepository(context, colorScheme) }
    var readSettings by remember {
        mutableStateOf(
            ReadSettings(
                textColor = colorScheme.onBackground.toArgb(),
                backgroundColor = colorScheme.background.toArgb()
            )
        )
    }

    // 收集阅读设置
    LaunchedEffect(Unit) {
        settingsRepository.readSettings.collectLatest { settings ->
            Log.d(TAG, "BookReadView:  $settings")
            readSettings = settings
        }
    }


    // 当界面销毁时恢复系统栏显示
    DisposableEffect(Unit) {
        onDispose {
            SystemBarUtils.showSystemBars(activity)
        }
    }

    // 分页逻辑
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    var availableHeight by remember { mutableStateOf(0.dp) }
    CommonPage(
        uiState = uiState,
        refresh = {
            viewModel.loadBookAndCatalog(bookId, chapterId)
        },
        navigator = navigator,
        modifier = Modifier
            .background(color = Color(readSettings.backgroundColor))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color(readSettings.backgroundColor))
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .fitInside(WindowInsetsRulers.DisplayCutout.current)
            ) {

                LaunchedEffect(Unit) {
                    viewModel.viewModelScope.launch(Dispatchers.Default) {
                        availableHeight = maxHeight
                        val height =
                            with(density) { maxHeight.toPx() - 36.sp.toPx() }
                        val availableWidth =
                            with(density) { maxWidth.toPx() - 16.dp.toPx() }
                        while (true) {
                            val it = viewModel.bookChapterChannel.receive()
                            measureText(
                                content = it.chapterContent,
                                density = density,
                                textMeasurer = textMeasurer,
                                readSettings = readSettings,
                                heightPx = height,
                                widthPx = availableWidth,
                                onCallBack = { indents, contents, startLines ->
                                    viewModel.addPages(
                                        it.chapterId,
                                        indents = indents,
                                        contents = contents,
                                        startLines = startLines
                                    )
                                }
                            )
                        }
                    }

                }
            }
            if (uiState.pages.isNotEmpty()) {
                InfiniteHorizontalPager(
                    items = uiState.pages,
                    modifier = Modifier
                        .fillMaxSize()
                        .fitInside(WindowInsetsRulers.DisplayCutout.current)
                        .clickable(
                            indication = null,
                            interactionSource = null,
                            onClick = { viewModel.toggleSystemBars() }),
                    onForward = {
                        viewModel.refreshPages(+1, it)
                        Log.d(TAG, "BookReadView: onForward $it")
                    },
                    initialItemIndex = uiState.currentPageIndex,
                    onBack = {
                        viewModel.refreshPages(-1, it)
                        Log.d(TAG, "BookReadView: onBack $it")
                    },
                    onPageChanged = { Log.d(TAG, "BookReadView:  pageChanged $it") }
                ) { page ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Row() {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "back",
                                tint = Color(readSettings.textColor),
                                modifier = Modifier
                                    .clickable {
                                        navigator.goBack()
                                    }
                                    .size(with(density) { 12.sp.toDp() }),
                            )
                            Text(
                                uiState.catalog[uiState.currentIndex].title,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Left,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    lineHeight = 12.sp,
                                    color = Color(readSettings.textColor),
                                )
                            )
                        }
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = Color(readSettings.textColor)
                        )
                        ChapterPage(
                            content = page.contents,
                            startLine = page.startLine,
                            settings = readSettings,
                            firstIndent = page.firstLineIndent,
                            modifier = Modifier
                                .weight(1f),
                            endClip = page.endClip,
                            commentCounts = uiState.chapterComments.mapValues { it.value.size },
                            onLineClick = { line, lineText ->
                                selectedCommentLine = line
                                selectedLineText = lineText
                            }
                        )
                    }
                }
            }
            AnimatedVisibility(
                uiState.showBottomControls,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(color = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // 目录抽屉
                    AnimatedVisibility(
                        uiState.showCatalog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(availableHeight * 2 / 3)
                    ) {
                        Column(
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = uiState.book?.cover,
                                        // 可选：添加占位/错误状态
                                        placeholder = painterResource(R.drawable.skeleton),
                                        error = painterResource(R.drawable.skeleton)
                                    ),
                                    contentDescription = uiState.book?.name,
                                    modifier = Modifier
                                        .height(60.dp),
                                    contentScale = ContentScale.FillHeight
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(
                                        uiState.book?.name ?: "",
                                        style = MaterialTheme.typography.titleLarge,
                                        maxLines = 1
                                    )
                                    Text(
                                        uiState.book?.author ?: "",
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1
                                    )
                                }
                                IconButton(onClick = { navigator.navigate(BookInfo(bookId = uiState.book?.id!!)) }) {
                                    Icon(Icons.Default.ChevronRight, "goToDetail")
                                }

                            }
                            CatalogDrawer(
                                modifier = Modifier
                                    .weight(1f),
                                isAscending = isAscending,
                                state = lazyListState,
                                catalog = uiState.catalog,
                                currentChapterId = uiState.catalog[uiState.currentIndex].id,
                                onChapterSelected = { chapterId ->
                                    viewModel.goChapterId(chapterId)
                                },
                                onDismiss = { viewModel.hideAllDialogs() },
                                onReverseCatalog = { isAscending = !isAscending }
                            )
                        }

                    }
                    AnimatedVisibility(uiState.showSettings) {
                        ReadingSettings(
                            settings = readSettings,
                            onSettingsChange = { newSettings ->
                                viewModel.viewModelScope.launch(Dispatchers.IO) {
                                    settingsRepository.updateSettings(newSettings)
                                }
                                viewModel.restart()
                            },
                        )
                    }
                    BottomControlBar(
                        onPreviousClick = { viewModel.goChapter(-1) },
                        onNextClick = { viewModel.goChapter(+1) },
                        onCatalogClick = {
                            viewModel.toggleCatalog()
                        },
                        onSettingsClick = { viewModel.toggleSettings() },
                        onDismiss = { viewModel.hideAllDialogs() },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    )
                }
            }


        }
    }

    // ===== 行评论底部弹层 =====
    selectedCommentLine?.let { line ->
        val chapterId = uiState.catalog.getOrNull(uiState.currentIndex)?.id ?: 0
        val comments = uiState.chapterComments[line].orEmpty()
        val currentUserId = AuthStore.user.value?.id
        ModalBottomSheet(
            onDismissRequest = { selectedCommentLine = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "第 ${line + 1} 行评论",
                    style = MaterialTheme.typography.titleMedium
                )
                if (selectedLineText.isNotBlank()) {
                    Text(
                        text = selectedLineText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HorizontalDivider()
                if (comments.isEmpty()) {
                    Text(
                        text = "暂无评论，快来发表第一条吧～",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        comments.forEach { comment ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = comment.userAvatar.ifEmpty { null }
                                        ),
                                        contentDescription = comment.userName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = comment.userName,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = comment.createdAt.split('T').firstOrNull() ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = comment.content,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (comment.userId == currentUserId) {
                                    TextButton(onClick = {
                                        if (chapterId > 0) {
                                            viewModel.deleteLineComment(chapterId, comment)
                                        }
                                    }) {
                                        Text(
                                            text = "删除",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it.take(2000) },
                    placeholder = { Text("写下你的看法（2000字以内）") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = {
                        if (chapterId > 0) {
                            viewModel.createLineComment(chapterId, line, commentInput) {
                                commentInput = ""
                            }
                        }
                    }) {
                        Text("发表评论")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterPage(
    modifier: Modifier = Modifier,
    content: List<String>,
    startLine: Int,
    settings: ReadSettings,
    firstIndent: Boolean = false,
    endClip: Boolean = false,
    commentCounts: Map<Int, Int> = emptyMap(),
    onLineClick: (Int, String) -> Unit
) {
    val icon = "ChatBubbleOutline"
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy((2 * settings.fontSize).dp)
    ) {
        content.forEachIndexed { index, paragraph ->
            // 当前段落在整个章节中的行号
            val line = startLine + index
            // 1. 构建 AnnotatedString，在段落末尾插入占位符
            val annotatedText = buildAnnotatedString {
                append(paragraph)
                // 插入内联内容的占位符
                if (index < content.size - 1 || !endClip) {
                    appendInlineContent(icon, alternateText = "[icon]")
                }
            }
            Text(
                text = annotatedText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                style = TextStyle(
                    color = Color(settings.textColor),
                    fontSize = settings.fontSize.sp,
                    lineHeight = settings.lineHeight.sp,
                    letterSpacing = settings.letterSpacing.sp,
                    textIndent = if (index == 0) {
                        if (firstIndent) {
                            TextIndent(firstLine = (settings.fontSize * 2).sp)
                        } else {
                            null
                        }

                    } else TextIndent(firstLine = (settings.fontSize * 2).sp),
                    fontFamily = settings.fontFamily,
                    textAlign = TextAlign.Justify
                ),
                inlineContent = mapOf(
                    icon to InlineTextContent(
                        placeholder = Placeholder(
                            width = settings.fontSize.sp,
                            height = settings.fontSize.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextBottom
                        )
                    ) {
                        Box(
                            modifier = Modifier.size(settings.fontSize.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Comment,
                                contentDescription = "查看本行评论",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onLineClick(line, paragraph) },
                                tint = Color(settings.textColor)
                            )
                            val count = commentCounts[line]
                            if (count != null && count > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color(0xFFDCA000), CircleShape)
                                        .padding(horizontal = 3.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (count > 99) "99+" else "$count",
                                        color = Color.White,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                    },
                )
            )
        }
    }
}