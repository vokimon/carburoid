package net.canvoki.carburoid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VerticalScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    thumbColor: Color = Color.Gray.copy(alpha = 0.5f),
) {
    Box(modifier = modifier.fillMaxHeight()) {
        Canvas(
            modifier =
                Modifier
                    .width(4.dp)
                    .fillMaxHeight(),
            onDraw = {
                val visibleItems = state.layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty()) {
                    val totalItems = state.layoutInfo.totalItemsCount
                    val viewportHeight = state.layoutInfo.viewportSize.height
                    val itemHeight = visibleItems[0].size

                    val totalContentHeight = totalItems * itemHeight
                    if (totalContentHeight > viewportHeight) {
                        val maxScrollOffset = totalContentHeight - viewportHeight
                        val currentScrollOffset =
                            state.firstVisibleItemIndex * itemHeight +
                                state.firstVisibleItemScrollOffset

                        val thumbHeight =
                            ((viewportHeight.toFloat() / totalContentHeight) * viewportHeight)
                                .coerceIn(20f, viewportHeight.toFloat())

                        val thumbOffset =
                            if (maxScrollOffset > 0) {
                                (currentScrollOffset.toFloat() / maxScrollOffset) * (viewportHeight - thumbHeight)
                            } else {
                                0f
                            }

                        drawRoundRect(
                            color = thumbColor,
                            topLeft = Offset(0f, thumbOffset),
                            size = Size(size.width, thumbHeight),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                        )
                    }
                }
            },
        )
    }
}
