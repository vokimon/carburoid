package net.canvoki.carburoid.plotnavigator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.Point
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberFloatLinearAxisModel
import net.canvoki.carburoid.model.GasStation

data class StationPoint(
    val item: GasStation,
    val index: Int,
    override val x: Float,
    override val y: Float,
) : Point<Float, Float>

fun xRange(points: List<StationPoint>): Pair<Float, Float> {
    val xMax = points.maxOfOrNull { it.x } ?: 0f
    return 0f to (if (xMax <= 0f) 800f else xMax)
}

fun yRange(points: List<StationPoint>): Pair<Float, Float> {
    val defaultMin = 0f
    val defaultMax = 2f
    val yMin = points.minOfOrNull { it.y }
    val yMax = points.maxOfOrNull { it.y }

    // No points
    if (yMin == null || yMax == null) {
        return defaultMin to defaultMax
    }

    // Regular case
    if (yMin < yMax) {
        return yMin to yMax
    }

    // both yMin and yMax at ot below zero
    if (yMax <= defaultMin) {
        return defaultMin to defaultMax
    }
    return defaultMin to yMax
}

@Composable
fun ScatterPlot(
    items: List<GasStation>,
    getX: (GasStation) -> Float?,
    getY: (GasStation) -> Float?,
    onIndexSelected: (Int) -> Unit,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    val currentIndex by rememberUpdatedState(selectedIndex)

    val points by remember(items, getX, getY) {
        derivedStateOf {
            items.mapIndexed { index, item ->
                val x = getX(item) ?: 0f
                val y = getY(item) ?: 0f
                StationPoint(item = item, x = x, y = y, index = index)
            }
        }
    }

    val (xMin, xMax) = xRange(points)
    val (yMin, yMax) = yRange(points)

    fun changePage(delta: Int) {
        val newIndex = (currentIndex + delta).coerceIn(0, points.lastIndex)
        onIndexSelected(newIndex)
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    XYGraph(
        xAxisModel = rememberFloatLinearAxisModel(xMin..xMax),
        yAxisModel = rememberFloatLinearAxisModel(yMin..yMax),
        xAxisTitle = "Distància (km)",
        yAxisTitle = null, //"Preu (€)",
        xAxisLabels = { "%.01f".format(it) },
        yAxisLabels = { "%.03f€".format(it) },
        modifier = modifier.horizontalSwipe(onStep = ::changePage),
    ) {
        // Vertical line across the selected item
        points.getOrNull(currentIndex)?.let { point ->
            LinePlot2(
                data =
                    listOf(
                        StationPoint(point.item, -1, point.x, yMin),
                        StationPoint(point.item, -1, point.x, yMax),
                    ),
                lineStyle =
                    LineStyle(
                        strokeWidth = 2.dp,
                        brush = SolidColor(colors.outline),
                    ),
                symbol = null, // no points
            )
        }

        // Relevant items as dots
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
                            .clickable {
                                onIndexSelected(point.index)
                            }.size(if (isSelected) 12.dp else 8.dp),
                )
            },
        )
    }
}
