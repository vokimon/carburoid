package net.canvoki.carburoid.plotnavigator

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.koalaplot.core.xygraph.XYGraphScope

typealias BinGrid<Z> = Array<Array<Z>>

@Composable
fun <X : Comparable<X>, Y : Comparable<Y>, Z> XYGraphScope<X, Y>.HeatMapPlot(
    xDomain: ClosedRange<X>,
    yDomain: ClosedRange<Y>,
    bins: BinGrid<Z>,
    getAlpha: (Z) -> Float,
    getColor: (Z) -> Color,
) {
    if (bins.isEmpty() || bins[0].isEmpty()) return

    val xBins = bins.size
    val yBins = bins[0].size

    val xMin = xDomain.start
    val xMax = xDomain.endInclusive
    val yMin = yDomain.start
    val yMax = yDomain.endInclusive

    for (xi in 0 until xBins) {
        for (yi in 0 until yBins) {
            val value = bins[xi][yi] ?: continue
            val alpha = getAlpha(value)
            if (alpha <= 0f) continue
            val color = getColor(value)

            val x1 = lerp(xMin, xMax, xi.toFloat() / xBins)
            val x2 = lerp(xMin, xMax, (xi + 1).toFloat() / xBins)
            val y1 = lerp(yMin, yMax, yi.toFloat() / yBins)
            val y2 = lerp(yMin, yMax, (yi + 1).toFloat() / yBins)

            Rectangle(
                x1 = x1,
                y1 = y1,
                x2 = x2,
                y2 = y2,
                alpha = alpha,
                color = color,
            )
        }
    }
}

/**
 * Linear interpolation between start and end of type T using Float ratio.
 * Only supports Float, Double, Int. Extend if needed.
 */
@Suppress("UNCHECKED_CAST")
private fun <T : Comparable<T>> lerp(
    start: T,
    end: T,
    ratio: Float,
): T =
    when (start) {
        is Float -> (start + (end as Float - start) * ratio) as T
        is Double -> (start + (end as Double - start) * ratio) as T
        is Int -> (start + ((end as Int - start) * ratio)).toInt() as T
        else -> throw IllegalArgumentException("Unsupported numeric type for lerp: ${start!!::class}")
    }
