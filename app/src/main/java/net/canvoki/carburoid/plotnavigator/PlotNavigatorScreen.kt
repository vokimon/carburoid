package net.canvoki.carburoid.plotnavigator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.ui.settings.ExperimentalFeatureNotice
import net.canvoki.carburoid.ui.settings.ThemeSettings

@Composable
fun PlotNavigatorScreen(
    stations: List<GasStation>,
    allStations: List<GasStation>,
) {
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
                    allItems = allStations,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        ExperimentalFeatureNotice(
            noticeId = "feature_plot_navigator_122",
            title = "Experimental screen",
            message =
                """This is an work-in-progress screen. """ +
                    """Here be dragons but your feed back is very wellcome. """ +
                    """How does it feel to you? How should it look like?""",
        )
    }
}
