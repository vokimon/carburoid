package net.canvoki.carburoid

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.canvoki.carburoid.algorithms.FilterSettings
import net.canvoki.carburoid.algorithms.StationFilter
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.timeits
import net.canvoki.carburoid.log

class MainSharedViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = (application as CarburoidApplication).repository

    private val _stationsReloadStarted = MutableSharedFlow<Unit>(replay = 0)
    val stationsReloadStarted: SharedFlow<Unit> = _stationsReloadStarted.asSharedFlow()

    private val _stationsUpdated = MutableSharedFlow<List<GasStation>>(replay = 0)
    val stationsUpdated: SharedFlow<List<GasStation>> = _stationsUpdated.asSharedFlow()

    init {
        // Observe filter changes
        viewModelScope.launch {
            FilterSettings.changes.collect {
                log("EVENT Filter updated")
                reloadStations()
            }
        }
    }

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

    fun reloadStations() {
        viewModelScope.launch {
            _stationsReloadStarted.emit(Unit)

            val stations =
                timeits("PROCESSING STATIONS") {
                    getStationsToDisplay()
                }

            _stationsUpdated.emit(stations)
        }
    }
}
