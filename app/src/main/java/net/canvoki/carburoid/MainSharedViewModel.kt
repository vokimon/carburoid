package net.canvoki.carburoid

import androidx.lifecycle.ViewModel
import net.canvoki.carburoid.repository.GasStationRepository
import net.canvoki.carburoid.model.GasStation

class MainSharedViewModel : ViewModel() {
    /**
     * Returns the current list of stations.
     * Preserves repository behavior: returns cached data immediately
     * and triggers fetch if expired.
     */
    fun getStations(repository: GasStationRepository): List<GasStation> {
        return repository.getData()?.stations ?: emptyList()
    }
}
