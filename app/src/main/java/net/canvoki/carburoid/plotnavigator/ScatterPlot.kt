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
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.Point
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberFloatLinearAxisModel
import net.canvoki.carburoid.log
import net.canvoki.carburoid.model.GasStation

data class StationPoint(
    val item: GasStation,
    val index: Int,
    override val x: Float,
    override val y: Float,
) : Point<Float, Float>

@Composable
fun ScatterPlot(
    items: List<GasStation>,
    getX: (GasStation) -> Float?,
    getY: (GasStation) -> Float?,
    onPointClick: (GasStation) -> Unit,
    selectedItem: GasStation? = null,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    log("Redraw ScatterPlot ${selectedItem?.id}")
    val currentSelectedItem by rememberUpdatedState(selectedItem)

    val points by remember(items, getX, getY) {
        derivedStateOf {
            items.mapIndexed { index, station ->
                val x = getX(station) ?: 0f
                val y = getY(station) ?: 0f
                StationPoint(item = station, x = x, y = y, index = index)
            }
        }
    }

    val xMin = 0f
    val xMax = points.maxOfOrNull { it.x } ?: 800f
    val yMin = points.minOfOrNull { it.y } ?: 0f
    val yMax = points.maxOfOrNull { it.y } ?: 2f

    fun changePage(delta: Int) {
        if (points.isEmpty()) return

        val currentIndex =
            currentSelectedItem?.let { item ->
                log("find ${item.id} on ${points.size} items")
                points
                    .firstOrNull {
                        log("compare ${it.item.id} ${it.index}")
                        it.item.id == item.id
                    }?.index
            } ?: 0

        val newIndex =
            (currentIndex + delta)
                .coerceIn(0, points.lastIndex)

        log("Deltaing $delta: ${currentSelectedItem?.id} [$currentIndex] -> ${points[newIndex]?.item?.id} [$newIndex]")
        if (newIndex != currentIndex) {
            onPointClick(points[newIndex].item)
        }
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
        selectedItem?.let { item ->
            val x = getX(item) ?: return@let

            LinePlot2(
                data =
                    listOf(
                        StationPoint(item, -1, x, yMin),
                        StationPoint(item, -1, x, yMax),
                    ),
                lineStyle =
                    LineStyle(
                        strokeWidth = 2.dp,
                        brush = SolidColor(colors.outline),
                    ),
                symbol = null, // no points
            )
        }
        LinePlot2(
            data = points,
            symbol = { point ->
                val stationPoint = point as StationPoint
                val isSelected = stationPoint.item.id == selectedItem?.id
                Symbol(
                    fillBrush = SolidColor(if (isSelected) colors.primary else colors.tertiary),
                    outlineBrush = SolidColor(if (isSelected) colors.onPrimary else colors.onTertiary),
                    modifier =
                        Modifier
                            .clickable {
                                val item = (point as StationPoint).item
                                log("Simbol Clicked ${item.id} ${item.name}")
                                onPointClick(item)
                            }.size(if (isSelected) 12.dp else 8.dp),
                )
            },
        )
    }
}
