package net.canvoki.carburoid.plotnavigator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import io.github.koalaplot.core.xygraph.XYGraphScope

/**
 * Draws an axis-aligned rectangle in domain (data) coordinates.
 */
@Composable
fun <X, Y> XYGraphScope<X, Y>.Rectangle(
    x1: X,
    y1: Y,
    x2: X,
    y2: Y,
    color: Color,
    alpha: Float = 1f,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Convert domain → normalized → pixel
        val nx1 = xAxisModel.computeOffset(x1)
        val nx2 = xAxisModel.computeOffset(x2)
        val ny1 = yAxisModel.computeOffset(y1)
        val ny2 = yAxisModel.computeOffset(y2)

        // Pixel coordinates
        val px1 = nx1 * size.width
        val px2 = nx2 * size.width

        // KoalaPlot Y=0 is bottom → Canvas Y=0 is top → flip with (1 - normalized)
        val py1 = (1f - ny1) * size.height
        val py2 = (1f - ny2) * size.height

        // Compute ordered rectangle edges
        val left = minOf(px1, px2)
        val right = maxOf(px1, px2)
        val top = minOf(py1, py2)
        val bottom = maxOf(py1, py2)

        val width = right - left
        val height = bottom - top

        if (width > 0f && height > 0f) {
            drawRect(
                color = color,
                topLeft =
                    androidx.compose.ui.geometry
                        .Offset(left, top),
                size = Size(width, height),
                alpha = alpha,
            )
        }
    }
}
