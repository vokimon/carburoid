package net.canvoki.carburoid.plotnavigator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import net.canvoki.carburoid.CarburoidApplication
import net.canvoki.carburoid.R
import net.canvoki.carburoid.model.GasStation
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

        setContentView(R.layout.activity_chart_view)

        val composeView = findViewById<ComposeView>(R.id.chartView)
        val stations = repository.getData()?.stations ?: emptyList()

        composeView.setContent {
            PortNavigatorScreen(stations)
        }
    }
}
