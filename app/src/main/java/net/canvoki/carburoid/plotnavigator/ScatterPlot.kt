package net.canvoki.carburoid.plotnavigator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.heatmap.HeatMapGrid
import io.github.koalaplot.core.heatmap.HeatMapPlot
import io.github.koalaplot.core.heatmap.generateHistogram2D
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.AxisContent
import io.github.koalaplot.core.xygraph.AxisStyle
import io.github.koalaplot.core.xygraph.HorizontalLineAnnotation
import io.github.koalaplot.core.xygraph.Point
import io.github.koalaplot.core.xygraph.VerticalLineAnnotation
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberFloatLinearAxisModel
import net.canvoki.carburoid.log
import net.canvoki.carburoid.model.GasStation

typealias RangeF = Pair<Float, Float>

data class StationPoint(
    val item: GasStation,
    val index: Int,
    override val x: Float,
    override val y: Float,
) : Point<Float, Float>

/**
 * Compute min/max range for X axis
 */
fun xRange(points: List<StationPoint>): RangeF {
    val xMax = points.maxOfOrNull { it.x } ?: 0f
    return 0f to (
        if (xMax <= 0f) {
            800f
        } else if (xMax > 1000f) {
            1000f
        } else {
            xMax
        }
    )
}

/**
 * Compute min/max range for Y axis
 */
fun yRange(points: List<StationPoint>): RangeF {
    val defaultMin = 0f
    val defaultMax = 2f
    val yMin = points.minOfOrNull { it.y }
    val yMax = points.maxOfOrNull { it.y }

    if (yMin == null || yMax == null) return defaultMin to defaultMax
    if (yMin < yMax) return yMin to yMax
    if (yMax <= defaultMin) return defaultMin to defaultMax
    return defaultMin to yMax
}

fun yRange(
    points: List<StationPoint>,
    allItems: List<GasStation>,
    getY: (GasStation) -> Float?,
): RangeF {
    val defaultMin = 0f
    val defaultMax = 2f

    // Local minimum remains based on visible points
    val localMin = points.minOfOrNull { it.y } ?: defaultMin

    // Global maximum comes ONLY from allItems
    val globalMax = allItems.mapNotNull(getY).maxOrNull() ?: defaultMax

    // Sanity rules
    if (localMin >= globalMax) {
        if (globalMax <= defaultMin) return defaultMin to defaultMax
        return defaultMin to globalMax
    }

    return localMin to globalMax
}

/**
 * Scatter plot with optional heatmap
 */
@Composable
fun ScatterPlot(
    items: List<GasStation>,
    allItems: List<GasStation>,
    getX: (GasStation) -> Float?,
    getY: (GasStation) -> Float?,
    onIndexSelected: (Int) -> Unit,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    nXBins: Int = 50,
    nYBins: Int = 50,
) {
    val colors = MaterialTheme.colorScheme
    val currentIndex by rememberUpdatedState(selectedIndex)

    val points by remember(items, getX, getY) {
        derivedStateOf {
            items.mapIndexed { index, item ->
                StationPoint(
                    item = item,
                    index = index,
                    x = getX(item) ?: 0f,
                    y = getY(item) ?: 0f,
                )
            }
        }
    }

    val histogram by remember(allItems, points, getX, getY) {
        derivedStateOf {
            val validItems = allItems.filter { getX(it) != null && getY(it) != null }

            val (xMin, xMax) = xRange(points)
            val (yMin, yMax) = yRange(points, allItems, getY)

            generateHistogram2D(
                samples = validItems,
                nBinsX = nXBins,
                nBinsY = nYBins,
                xGetter = { getX(it)!! },
                yGetter = { getY(it)!! },
                xDomain = xMin..xMax,
                yDomain = yMin..yMax,
            )
        }
    }

    val (xMin, xMax) = xRange(points)
    val (yMin, yMax) = yRange(points, allItems, getY)

    @OptIn(ExperimentalKoalaPlotApi::class)
    XYGraph(
        xAxisModel = rememberFloatLinearAxisModel(xMin..xMax),
        yAxisModel = rememberFloatLinearAxisModel(yMin..yMax),
        xAxisContent =
            AxisContent(
                title = { null },
                labels = { it.toInt() },
                style = AxisStyle(),
            ),
        yAxisContent =
            AxisContent(
                title = { null },
                labels = { "%.02f€".format(it) },
                style = AxisStyle(),
            ),
        modifier =
            modifier.horizontalSwipe(onStep = { delta ->
                onIndexSelected((currentIndex + delta).coerceIn(0, points.lastIndex))
            }),
    ) {
        val bins: HeatMapGrid<Int> = histogram
        val maxFrequency = bins.flatten().maxOrNull()?.takeIf { it != 0 } ?: 1

        histogram.firstOrNull()?.let {
            HeatMapPlot(
                xDomain = xMin..xMax,
                yDomain = yMin..yMax,
                bins = histogram,
                alphaScale = { it * 0.9f / maxFrequency },
                colorScale = { colors.outline },
            )
        }

        val currentPoint = points.getOrNull(currentIndex)

        currentPoint?.let {
            HorizontalLineAnnotation(it.y, LineStyle(SolidColor(colors.tertiary)))
            VerticalLineAnnotation(it.x, LineStyle(SolidColor(colors.tertiary)))
        }

        LinePlot2(
            data = points,
            symbol = { point ->
                point as StationPoint
                val isSelected = point.index == currentIndex
                Symbol(
                    fillBrush = SolidColor(if (isSelected) colors.primary else colors.tertiary),
                    outlineBrush = SolidColor(if (isSelected) colors.onPrimary else colors.onTertiary),
                    modifier =
                        Modifier
                            .clickable { onIndexSelected(point.index) }
                            .size(if (isSelected) 12.dp else 8.dp),
                )
            },
        )
    }
}
