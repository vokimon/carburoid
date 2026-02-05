// ui/components/LazyColumn.kt
package net.canvoki.carburoid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.lazy.LazyColumn as AndroidXLazyColumn

/**
 * Drop-in replacement for [androidx.compose.foundation.lazy.LazyColumn]
 * with subtle fade indicators.
 *
 * Automatically adds padding proportional to [fadeHeight] to ensure fades are visible,
 * and adds it to any user-provided [contentPadding].
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

    val combinedPadding =
        PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection),
            top = fadeHeight + contentPadding.calculateTopPadding(),
            end = contentPadding.calculateEndPadding(layoutDirection),
            bottom = (fadeHeight * 6) + contentPadding.calculateBottomPadding(),
        )

    Box(modifier = modifier) {
        AndroidXLazyColumn(
            state = state,
            contentPadding = combinedPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            modifier = Modifier.fillMaxSize(),
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
