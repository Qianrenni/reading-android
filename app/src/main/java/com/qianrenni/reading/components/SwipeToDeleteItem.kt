package com.qianrenni.reading.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 左滑后露出的删除按钮宽度。 */
private val REVEAL_WIDTH = 72.dp

/** 展开/回弹的动画时长。 */
private const val SETTLE_DURATION_MS = 200

/** 删除区域的无障碍描述（色块内无内容，UI 测试也依赖该标识）。 */
private const val DELETE_LABEL = "删除"

/**
 * 删除色块颜色。
 *
 * 不用 [MaterialTheme.colorScheme.error]：深色主题下它是 #CF6679，偏粉。
 * 删除属于危险操作，这里固定用正红色，明暗主题表现一致。
 */
private val DELETE_COLOR = Color(0xFFD32F2F)

/**
 * 左滑露出右侧删除按钮的列表项容器。
 *
 * 手势只处理水平方向，纵向拖动仍交给外层 LazyColumn；内容层不透明，
 * 未滑动时删除按钮完全被盖住，滑动超过一半松手会自动吸附到删除态。
 *
 * @param onDelete 点击露出的删除按钮时回调
 */
@Composable
fun SwipeToDeleteItem(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val revealWidthPx = with(LocalDensity.current) { REVEAL_WIDTH.toPx() }
    val offsetX = remember { Animatable(0f) }

    Box(modifier = modifier.clipToBounds()) {
        // 底层：藏在右侧的删除区域（纯色块，内部不放置任何内容）
        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(REVEAL_WIDTH)
                    .fillMaxHeight()
                    .background(DELETE_COLOR)
                    .clickable {
                        scope.launch { offsetX.snapTo(0f) }
                        onDelete()
                    }
                    .semantics { contentDescription = DELETE_LABEL }
            )
        }

        // 上层：内容随手指水平移动
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .background(MaterialTheme.colorScheme.background)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            offsetX.snapTo((offsetX.value + delta).coerceIn(-revealWidthPx, 0f))
                        }
                    },
                    onDragStopped = {
                        offsetX.animateTo(
                            targetValue = if (offsetX.value < -revealWidthPx / 2) -revealWidthPx else 0f,
                            animationSpec = tween(durationMillis = SETTLE_DURATION_MS)
                        )
                    }
                )
        ) {
            content()
        }
    }
}
