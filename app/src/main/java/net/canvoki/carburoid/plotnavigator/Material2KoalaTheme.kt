package net.canvoki.carburoid.plotnavigator

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.style.KoalaPlotTheme

/**
 * Wrapper that applies MaterialTheme to KoalaTheme
 */
@Composable
fun Material2KoalaTheme(
    content: @Composable () -> Unit
) {
    val materialColors = MaterialTheme.colorScheme
    KoalaPlotTheme(
        axis = KoalaPlotTheme.axis.copy(
            color = materialColors.onBackground,
            majorGridlineStyle = LineStyle(
                brush = SolidColor(materialColors.outlineVariant),
                alpha = 0.5f,
                strokeWidth = 1.0.dp
            ),
            minorGridlineStyle = LineStyle(
                brush = SolidColor(materialColors.outlineVariant),
                alpha = 0.2f,
                strokeWidth = 1.0.dp
            ),
        ),
    ) {
        content()

    }
}
