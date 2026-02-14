package net.canvoki.carburoid.plotnavigator

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import net.canvoki.carburoid.MainSharedViewModel
import net.canvoki.carburoid.model.GasStation

class PlotNavigatorActivity : AppCompatActivity() {
    private val viewModel: MainSharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Required in Android 16+
        //WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val stations by viewModel.stationsUpdated.collectAsState(
                initial = viewModel.getStationsToDisplay(),
            )
            val allStations by viewModel.rawStationsUpdated.collectAsState(
                initial = viewModel.getStations(),
            )

            PlotNavigatorScreen(stations, allStations)
        }
    }
}
