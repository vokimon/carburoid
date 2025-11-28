package net.canvoki.carburoid

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import net.canvoki.carburoid.algorithms.FilterSettings
import net.canvoki.carburoid.algorithms.StationFilter
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.timeits

class MainSharedViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = (application as CarburoidApplication).repository

    /**
     * Returns the current list of stations.
     * Preserves repository behavior: returns cached data immediately
     * and triggers fetch if expired.
     */
    fun getStations(): List<GasStation> = repository.getData()?.stations ?: emptyList()

    /**
     * Returns stations filtered according to the given configuration.
     */
    fun getStationsToDisplay(): List<GasStation> {
        val stations = getStations()
        val config = FilterSettings.config(getApplication())
        return timeits("PROCESSING STATIONS") {
            StationFilter(config).filter(stations)
        }
    }
}
