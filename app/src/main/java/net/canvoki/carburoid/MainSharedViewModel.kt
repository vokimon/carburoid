package net.canvoki.carburoid

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.repository.GasStationRepository

class MainSharedViewModel(
    application: CarburoidApplication,
) : AndroidViewModel(application) {
    private val repository = application.repository

    /**
     * Returns the current list of stations.
     * Preserves repository behavior: returns cached data immediately
     * and triggers fetch if expired.
     */
    fun getStations(): List<GasStation> = repository.getData()?.stations ?: emptyList()
}
