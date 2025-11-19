package net.canvoki.carburoid.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
//import com.patrykandpatrick.vico.core.cartesian.data.Entry
//import com.patrykandpatrick.vico.core.cartesian.data.EntryModel
//import com.patrykandpatrick.vico.core.entry.entryOf
//import com.patrykandpatrick.vico.core.chart.painter.PointDrawer
//import com.patrykandpatrick.vico.core.chart.painter.shape.CircleShape as VicoCircleShape
import net.canvoki.carburoid.model.GasStation

@Composable
fun GasStationScatterPlot(gasStations: List<GasStation>, modifier: Modifier = Modifier) {
    var selectedStation by remember { mutableStateOf<GasStation?>(null) }

    // Filter out stations with nulls
    val entries = gasStations.mapNotNull { station ->
        val x = station.distanceInMeters ?: return@mapNotNull null
        val y = station.price?.toFloat() ?: return@mapNotNull null
        entryOf(x.toFloat(), y, station) // attach station as `data` payload
    }

    val lineLayer = LineCartesianLayer(
        pointDrawer = PointDrawer(shape = VicoCircleShape, size = 8.dp),
        onPointClick = { entry ->
            selectedStation = entry.data as? GasStation
        }
    )

    val chartModel = CartesianChartModel(
        layers = listOf(
            LineCartesianLayerModel(entries = entries, lineLayer = lineLayer)
        )
    )

    Column(modifier) {
        CartesianChartHost(
            chart = chartModel,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        selectedStation?.let { station ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Name: ${station.name}")
                    Text("Price: €${station.price}")
                    Text("Distance: ${station.distance} km")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        // Navigate to detail activity
                    }) {
                        Text("Go to Detail")
                    }
                }
            }
        }
    }
}
