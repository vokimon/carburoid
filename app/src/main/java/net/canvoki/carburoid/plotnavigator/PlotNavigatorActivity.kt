package net.canvoki.carburoid.plotnavigator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import net.canvoki.carburoid.CarburoidApplication
import net.canvoki.carburoid.repository.GasStationRepository
import net.canvoki.carburoid.ui.settings.ThemeSettings

class PlotNavigatorActivity : ComponentActivity() {
    private val app: CarburoidApplication
        get() = application as CarburoidApplication

    private val repository: GasStationRepository
        get() = app.repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Required in Android 16+
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val stations = repository.getData()?.stations ?: emptyList()

        setContent {
            PortNavigatorScreen(stations)
        }
    }
}
