package net.canvoki.carburoid.plotnavigator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.product.CategorizedProductSelector
import net.canvoki.carburoid.ui.settings.ThemeSettings

@Composable
fun PlotNavigatorScreen(stations: List<GasStation>) {
    MaterialTheme(
        colorScheme = ThemeSettings.effectiveColorScheme(),
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
        ) { padding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
            ) {
                GasStationScatterPlot(
                    items = stations,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
fun GasStationScatterPlot(
    items: List<GasStation>,
    modifier: Modifier = Modifier,
) {
    // Local functions to get X and Y values for the plot
    fun getX(station: GasStation): Float? = station.distanceInMeters?.div(1000.0f)

    fun getY(station: GasStation): Float? = station.prices["Gasoleo A"]?.toFloat()

    var selectedItem by remember { mutableStateOf<GasStation?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Column {
            CategorizedProductSelector()
            GasStationCard(selectedItem)
        }
        Material2KoalaTheme {
            ScatterPlot(
                items = items,
                getX = ::getX,
                getY = ::getY,
                selectedItem = selectedItem,
                onPointClick = { selectedItem = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
