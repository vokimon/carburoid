package net.canvoki.carburoid

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import java.io.File
import java.io.FileNotFoundException
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.canvoki.carburoid.repository.GasStationRepository
import net.canvoki.carburoid.network.GasStationApiFactory
import net.canvoki.carburoid.model.GasStationResponse
import net.canvoki.carburoid.ui.applyPreferencesTheme

class CarburoidApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit private var cacheFile: File
    lateinit var repository: GasStationRepository
        private set

    override fun onCreate() {
        super.onCreate()
        applyPreferencesTheme(this)
        cacheFile = File(filesDir, "gas_stations_cache.json")
        setupDebugData()
        repository = setupRepository()
    }

    fun setupDebugData() {
        if (cacheFile.exists()) return
        val mockJson = try {
            assets.open("stations-debug.json").bufferedReader().use { it.readText() }
        } catch (e: FileNotFoundException) {
            log("MOCK DATA NOT FOUND. RELEASE?")
            return
        }
        log("USING MOCK DATA. DEBUG?")
        cacheFile.writeText(mockJson)
    }

    fun setupRepository() : GasStationRepository {
        val api = GasStationApiFactory.create()
        val repository = GasStationRepository(
            api = api,
            cacheFile = cacheFile,
            scope = appScope,
        )
        return repository
    }
}

