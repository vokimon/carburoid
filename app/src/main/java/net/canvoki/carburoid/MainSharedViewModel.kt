package net.canvoki.carburoid

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.canvoki.carburoid.algorithms.FilterSettings
import net.canvoki.carburoid.algorithms.StationFilter
import net.canvoki.carburoid.distances.CurrentDistancePolicy
import net.canvoki.carburoid.log
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.product.ProductManager
import net.canvoki.carburoid.repository.RepositoryEvent
import net.canvoki.carburoid.timeits

class MainSharedViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = (application as CarburoidApplication).repository

    private val _stationsReloadStarted = MutableSharedFlow<Unit>(replay = 0)
    val stationsReloadStarted: SharedFlow<Unit> = _stationsReloadStarted.asSharedFlow()

    private val _stationsUpdated = MutableSharedFlow<List<GasStation>>(replay = 0)
    val stationsUpdated: SharedFlow<List<GasStation>> = _stationsUpdated.asSharedFlow()

    private val _rawStationsUpdated = MutableSharedFlow<List<GasStation>>(replay = 0)
    val rawStationsUpdated: SharedFlow<List<GasStation>> = _rawStationsUpdated.asSharedFlow()

    private var _stationsToDisplay: List<GasStation> = emptyList()
    private var reloadJob: Job? = null

    init {

        // Observe product changes
        viewModelScope.launch {
            ProductManager.productChanged.collect {
                log("VM EVENT product updated")
                reloadStations("Product change")
            }
        }
        // Observe changes on how to compute distance
        viewModelScope.launch {
            CurrentDistancePolicy.methodChanged.collect {
                log("VM EVENT Distance policy updated")
                reloadStations("Distance policy change")
            }
        }
        // Observe filter changes
        viewModelScope.launch {
            FilterSettings.changes.collect {
                log("VM EVENT Filter updated")
                reloadStations("Filters change")
            }
        }
        // Observe repository updates
        viewModelScope.launch {
            repository.events.collect { event ->
                when (event) {
                    is RepositoryEvent.UpdateReady -> {
                        log("VM EVENT Repository.UpdateReady")
                        _rawStationsUpdated.emit(getStations())
                        reloadStations("Data change")
                    }
                    else -> Unit
                }
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
    fun getStationsToDisplay(): List<GasStation> = _stationsToDisplay

    fun reloadStations(reason: String = "Unknonw") {
        // Cancel any existing job
        reloadJob?.cancel()

        reloadJob = viewModelScope.launch {
            _stationsReloadStarted.emit(Unit)

            val stations = getStations()
            val config = FilterSettings.config(getApplication())
            val newStations =
                withContext(Dispatchers.Default) {
                    timeits("PROCESSING STATIONS $reason") {
                        Thread.sleep(2000)
                        StationFilter(config).filter(stations)
                    }
                }
            // Only update if the job has not been cancelled
            ensureActive()
            _stationsToDisplay = newStations

            _stationsUpdated.emit(_stationsToDisplay)
        }
    }
}
