
package net.canvoki.carburoid.ui
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.component.ShapeComponent
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.math.BigDecimal
import java.math.RoundingMode
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.log

@Composable
fun GasStationSummaryCard(station: GasStation?) {
    if (station == null) return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
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
}

fun getX(station: GasStation): Float? = station.distanceInMeters?.div(1000.0f)
fun getY(station: GasStation): Float? = station.prices["Gasoleo A"]?.toFloat()

@Composable
fun GasStationScatterPlot(
    stations: List<GasStation>,
    modifier: Modifier = Modifier
) {
    var selectedStation by remember { mutableStateOf<GasStation?>(null) }
    val validStations = stations.filter {
        getX(it) != null && getY(it) != null
    }
    val modelProducer = remember(validStations) { CartesianChartModelProducer() }

    LaunchedEffect(validStations) {
        val xValues = validStations.map { getX(it)!! }
        val yValues = validStations.map { getY(it)!! }
        log("${xValues.size} ${yValues.size}")
        modelProducer.runTransaction {
            lineSeries {
                series(
                    x = xValues,
                    y = yValues,
                )
            }
        }
    }

    val pointLayer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(
            LineCartesianLayer.Line(
                fill = LineCartesianLayer.LineFill.single(fill(Color.Transparent)),
                //stroke = LineCartesianLayer.LineStroke.solid(Color.Transparent),
                pointProvider = LineCartesianLayer.PointProvider.single(
                    LineCartesianLayer.Point(
                        rememberShapeComponent(Fill(0x77ff0000 ), CorneredShape.Pill),
                    ),
                )
            )
        )
    )

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                pointLayer, //rememberLineCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(),
            ),
            modelProducer = modelProducer,
            modifier = modifier,
        )
        GasStationSummaryCard(stations[0]) // selectedStation
    }
}
