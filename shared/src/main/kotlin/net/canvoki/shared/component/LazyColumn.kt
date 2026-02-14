// ui/components/LazyColumn.kt
package net.canvoki.shared.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn as AndroidXLazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Drop-in replacement for [androidx.compose.foundation.lazy.LazyColumn]
 * with subtle indicators of scrollability.
 */
@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues.Zero,
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    fadeHeight: Dp = 14.dp,
    fadeAlpha: Float = 0.5f,
    content: LazyListScope.() -> Unit,
) {
    // Compensate the fade with content padding
    val layoutDirection = LocalLayoutDirection.current

    Box(modifier = modifier) {
        AndroidXLazyColumn(
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )

        val showTop by remember(state) {
            derivedStateOf { state.canScrollBackward }
        }
        val showBottom by remember(state) {
            derivedStateOf { state.canScrollForward }
        }

        val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = fadeAlpha)

        if (showTop) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(fadeHeight)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(surfaceColor, Color.Transparent),
                            ),
                        ),
            )
        }

        if (showBottom) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(fadeHeight)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, surfaceColor),
                            ),
                        ),
            )
        }
    }
}
