package net.canvoki.carburoid.plotnavigator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.autoScaleXRange
import io.github.koalaplot.core.xygraph.autoScaleYRange
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import io.github.koalaplot.core.xygraph.Point
import io.github.koalaplot.core.xygraph.rememberFloatLinearAxisModel
import io.github.koalaplot.core.xygraph.XYGraph
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.ui.settings.ThemeSettings
import net.canvoki.carburoid.log



fun getX(station: GasStation): Float? = station.distanceInMeters?.div(1000.0f)
fun getY(station: GasStation): Float? = station.prices["Gasoleo A"]?.toFloat()

@Composable
fun PortNavigatorScreen(stations: List<GasStation>) {
    MaterialTheme(
        colorScheme = ThemeSettings.effectiveColorScheme(),
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                GasStationScatterPlot(
                    items = stations,
                    getX = ::getX,
                    getY = ::getY,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
@Composable
fun GasStationScatterPlot(
    items: List<GasStation>,
    getX: (GasStation) -> Float?,
    getY: (GasStation) -> Float?,
    modifier: Modifier = Modifier,
) {
    var selectedItem by remember { mutableStateOf<GasStation?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Material2KoalaTheme {
            GasStationCard(selectedItem)
            ScatterPlot(
                items = items,
                getX = getX,
                getY = getY,
                selectedItem = selectedItem,
                onPointClick = { selectedItem = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun GasStationSummaryCard(station: GasStation?) {
    if (station == null) return
    log("Redraw Summary ${station.id}")
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Name: ${station.name}", style = MaterialTheme.typography.titleMedium)
        Text("Price: €${"%.3f".format(station.prices["Gasoleo A"] ?: 0.0f)}")
        Text("Distance: ${"%.1f".format((station.distanceInMeters ?: 0.0f) / 1000.0f)} km")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            // TODO: navigate to detail activity
        }) {
            Text("Go to Detail")
        }
    }
}

data class StationPoint(
    val item: GasStation,
    val index: Int,
    override val x: Float,
    override val y: Float,
) : Point<Float,Float>


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
    val valid by remember(items, getX, getY) {
        derivedStateOf {
            var minPrice = 10000f
            var index = 0
            items.mapNotNull {
                val x = getX(it) ?: return@mapNotNull null
                val y = getY(it) ?: return@mapNotNull null
                if (x>1000f) return@mapNotNull null
                it to (x to y)
            }
            .sortedBy { it.second.first }
            .mapNotNull {
                val item = it.first
                val x = it.second.first
                val y = it.second.second
                if (y>minPrice) return@mapNotNull null
                minPrice = y
                StationPoint(item=item, x=x, y=y, index=index++)
            }
        }
    }

    val xValues = valid.map { it.x }
    val yValues = valid.map { it.y }

    fun changePage(delta: Int) {
        if (valid.isEmpty()) return

        val currentIndex = currentSelectedItem?.let { item ->
            log("find ${item.id} on ${valid.size} items")
            valid.firstOrNull {
                log("compare ${it.item.id} ${it.index}")
                it.item.id == item.id
            } ?.index
        } ?: 0

        val newIndex = (currentIndex + delta)
            .coerceIn(0, valid.lastIndex)

        log("Deltaing ${delta}: ${currentSelectedItem?.id} [${currentIndex}] -> ${valid[newIndex]?.item?.id} [${newIndex}]")
        if (newIndex != currentIndex) {
            onPointClick(valid[newIndex].item)
        }
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    XYGraph(
        xAxisModel = rememberFloatLinearAxisModel(0.0f..xValues.max()),
        yAxisModel = rememberFloatLinearAxisModel(yValues.min()..yValues.max()),
        xAxisTitle = "Distància (km)",
        yAxisTitle = null, //"Preu (€)",
        xAxisLabels = { "%.01f".format(it)},
        yAxisLabels = { "%.03f€".format(it)},
        modifier = modifier.horizontalSwipe(onStep=::changePage),
    ) {
        selectedItem?.let { item ->
            val x = getX(item) ?: return@let

            LinePlot2(
                data = listOf(
                    StationPoint(item, -1, x, yValues.min()),
                    StationPoint(item, -1, x, yValues.max())
                ),
                lineStyle = LineStyle(
                    strokeWidth = 2.dp,
                    brush = SolidColor(colors.outline),
                ),
                symbol = null // sense punts
            )
        }
        LinePlot2(
            data = valid,
            symbol = { point ->
                val stationPoint = point as StationPoint
                val isSelected = stationPoint.item.id == selectedItem?.id
                Symbol(
                    fillBrush = SolidColor(if (isSelected) colors.primary else colors.tertiary),
                    outlineBrush = SolidColor(if (isSelected) colors.onPrimary else colors.onTertiary),
                    modifier = Modifier.clickable {
                        val item = (point as StationPoint).item
                        log("Simbol Clicked ${item.id} ${item.name}")
                        onPointClick(item)
                    }
                    .size(if (isSelected) 12.dp else 8.dp)

                )
            },
        )
    }
}

