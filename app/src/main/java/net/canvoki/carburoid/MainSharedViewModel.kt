package net.canvoki.carburoid

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import net.canvoki.carburoid.model.GasStation

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
}
