package net.canvoki.carburoid.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import net.canvoki.carburoid.CarburoidApplication
import net.canvoki.carburoid.ui.GasStationScatterPlot
import net.canvoki.carburoid.R
import net.canvoki.carburoid.repository.GasStationRepository

class ChartActivity : ComponentActivity() {
    private val app: CarburoidApplication
        get() = application as CarburoidApplication

    private val repository: GasStationRepository
        get() = app.repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val composeView = findViewById<ComposeView>(R.id.chartView)

        val stations = repository.getData()?.stations ?: emptyList()

        composeView.setContent {
            MaterialTheme {
                GasStationScatterPlot(stations)
            }
        }
    }
}

