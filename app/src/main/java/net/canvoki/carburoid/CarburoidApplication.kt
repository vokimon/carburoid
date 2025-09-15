package net.canvoki.carburoid

import android.app.Application
import android.content.Context
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.canvoki.carburoid.repository.GasStationRepository
import net.canvoki.carburoid.network.GasStationApiFactory
import net.canvoki.carburoid.model.GasStationResponse

class CarburoidApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var repository: GasStationRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = setupRepository()
    }

    fun setupRepository() : GasStationRepository {
        val cacheFile = File(filesDir, "gas_stations_cache.json")
        val api = GasStationApiFactory.create()
        val repository = GasStationRepository(
            api = api,
            cacheFile = cacheFile,
            scope = appScope,
        )
        return repository
    }
}

