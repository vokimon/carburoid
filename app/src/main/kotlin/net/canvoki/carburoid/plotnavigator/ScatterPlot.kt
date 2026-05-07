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
import io.github.koalaplot.core.xygraph.HorizontalLineAnnotation
import io.github.koalaplot.core.xygraph.Point
import io.github.koalaplot.core.xygraph.TickPosition
import io.github.koalaplot.core.xygraph.VerticalLineAnnotation
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberAxisStyle
import io.github.koalaplot.core.xygraph.rememberFloatLinearAxisModel
import net.canvoki.carburoid.model.GasStation
import net.canvoki.shared.log
import kotlin.math.abs

typealias RangeF = Pair<Float, Float>

private const val DEFAULT_DISPLAY_KM = 800f
private const val CUTOFF_DISPLAY_KM = 1000f

private const val MIN_PRICE_PADDING_EUR = 0.01f
private const val PADDING_RATIO = 0.05f
private const val ZERO_RANGE_EPSILON = 0.001f
private const val MIN_RANGE_EUR = 0.5f
private const val DEFAULT_Y_MIN = 0f
private const val DEFAULT_Y_MAX = 2f

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

    if (xMax <= 0f) return 0f to DEFAULT_DISPLAY_KM

    // Single point: double the max so point appears centered
    if (points.size <= 1) return 0f to (2 * xMax)

    // Only cap at CUTOFF_DISPLAY_KM if max exceeds it and there are nearby points
    if (xMax > CUTOFF_DISPLAY_KM) {
        val hasNearbyPoints = points.any { it.x <= CUTOFF_DISPLAY_KM }
        if (hasNearbyPoints) return 0f to CUTOFF_DISPLAY_KM
    }

    return 0f to xMax
}

fun yRange(
    points: List<StationPoint>,
    allItems: List<GasStation>,
    getY: (GasStation) -> Float?,
): RangeF {
    val filteredPrices = points.map { it.y }
    val allPrices = allItems.mapNotNull(getY)
    val unionPrices = filteredPrices + allPrices

    var unionMin = unionPrices.minOrNull()!!
    var unionMax = unionPrices.maxOrNull()!!

    // No points, default range
    if (unionMax == null || unionMin == null) {
        return DEFAULT_Y_MIN to DEFAULT_Y_MAX
    }

    // Single price, center vertically
    if (abs(unionMax - unionMin) < ZERO_RANGE_EPSILON) {
        return 0f to unionMax * 2f
    }

    // Regular set, just add some padding to the actual range
    val padding = maxOf(MIN_PRICE_PADDING_EUR, (unionMax - unionMin) * PADDING_RATIO)
    return unionMin - padding to unionMax + padding
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
                style =
                    rememberAxisStyle(
                        tickPosition = TickPosition.Inside,
                    ),
            ),
        yAxisContent =
            AxisContent(
                title = { null },
                labels = { "%.02f€".format(it) },
                style = rememberAxisStyle(),
            ),
        modifier =
            modifier.horizontalSwipe(onStep = { delta ->
                if (points.isNotEmpty()) {
                    onIndexSelected((currentIndex + delta).coerceIn(0, points.lastIndex))
                }
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
