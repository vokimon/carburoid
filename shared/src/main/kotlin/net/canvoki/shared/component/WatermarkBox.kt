package net.canvoki.shared.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

/**
 * A container that displays a watermark icon in the background with foreground content.
 *
 * The content does NOT inherit the parent modifier, preventing double-application
 * of modifiers like fillMaxSize() or padding.
 *
 * @param modifier Modifier applied to the root Box
 * @param watermark Painter for the background watermark icon
 * @param alpha Alpha value for the watermark (default: 0.1f)
 * @param tint Color tint for the watermark (default: onSurface from theme)
 * @param contentAlignment Alignment of the foreground content
 * @param contentPadding Padding around the content
 * @param content Foreground content composable
 */
@Composable
fun WatermarkBox(
    modifier: Modifier = Modifier,
    watermark: Painter,
    alpha: Float = 0.1f,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    contentAlignment: Alignment = Alignment.Center,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.padding(contentPadding),
        contentAlignment = contentAlignment,
    ) {
        // Watermark icon in background
        Icon(
            painter = watermark,
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxSize()
                    .alpha(alpha),
            tint = tint,
        )
        // Foreground content — does NOT inherit parent modifier
        content()
    }
}
