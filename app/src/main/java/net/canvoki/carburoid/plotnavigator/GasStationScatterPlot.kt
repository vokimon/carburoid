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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.product.CategorizedProductSelector
import net.canvoki.carburoid.product.ProductManager
import net.canvoki.carburoid.ui.settings.ExperimentalFeatureNotice
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
        ExperimentalFeatureNotice(
            noticeId = "feature_plot_navitator_bla2",
            message = "This is an experimental interface. Feedback on your experience using it is very welcome.",
        )
    }
}

@Composable
fun GasStationScatterPlot(
    items: List<GasStation>,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableStateOf(0) }
    val selectedItem by derivedStateOf {
        items.getOrNull(selectedIndex)
    }

    val product by rememberUpdatedState(ProductManager.getCurrent())

    LaunchedEffect(items) {
        selectedIndex = 0
    }

    fun getX(station: GasStation): Float? = station.distanceInMeters?.div(1000.0f)

    fun getY(station: GasStation): Float? = station.prices[product]?.toFloat()

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
                selectedIndex = selectedIndex,
                onIndexSelected = { index ->
                    selectedIndex = index
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
