package net.canvoki.carburoid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VerticalScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    thumbColor: Color = Color.Gray.copy(alpha = 0.5f),
) {
    val visibleItems = state.layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return

    val totalItems = state.layoutInfo.totalItemsCount
    val viewportHeight = state.layoutInfo.viewportSize.height
    val itemHeight = visibleItems[0].size // assume uniform height

    val totalContentHeight = totalItems * itemHeight
    if (totalContentHeight <= viewportHeight) return // no scrollbar needed

    val maxScrollOffset = totalContentHeight - viewportHeight

    // Current scroll offset from top
    val currentScrollOffset =
        state.firstVisibleItemIndex * itemHeight +
            state.firstVisibleItemScrollOffset

    // Thumb dimensions
    val thumbHeight =
        ((viewportHeight.toFloat() / totalContentHeight) * viewportHeight)
            .coerceIn(20f, viewportHeight.toFloat())

    // Thumb position (0 = top, viewportHeight - thumbHeight = bottom)
    val thumbOffset =
        if (maxScrollOffset > 0) {
            (currentScrollOffset.toFloat() / maxScrollOffset) * (viewportHeight - thumbHeight)
        } else {
            0f
        }

    Box(modifier = modifier.fillMaxHeight()) {
        Canvas(
            modifier =
                Modifier
                    .width(4.dp)
                    .fillMaxHeight(),
            onDraw = {
                drawRoundRect(
                    color = thumbColor,
                    topLeft =
                        androidx.compose.ui.geometry
                            .Offset(0f, thumbOffset),
                    size =
                        androidx.compose.ui.geometry
                            .Size(size.width, thumbHeight),
                    cornerRadius =
                        androidx.compose.ui.geometry
                            .CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                )
            },
        )
    }
}
