package net.canvoki.carburoid.plotnavigator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import net.canvoki.carburoid.R
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.ui.AppScaffold
import net.canvoki.carburoid.ui.settings.ExperimentalFeatureNotice

@Composable
fun PlotNavigatorScreen(
    stations: List<GasStation>,
    allStations: List<GasStation>,
    onPlotNavigatorClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    AppScaffold {
        GasStationScatterPlot(
            items = stations,
            allItems = allStations,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
