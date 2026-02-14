package net.canvoki.carburoid

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.canvoki.carburoid.country.CountryRegistry
import net.canvoki.carburoid.country.CountrySettings
import net.canvoki.carburoid.location.LocationService
import net.canvoki.carburoid.model.FranceExtraStationData
import net.canvoki.carburoid.network.GasStationApi
import net.canvoki.carburoid.repository.GasStationRepository
import net.canvoki.shared.settings.LanguageSettings
import net.canvoki.shared.settings.ThemeSettings
import java.io.File
import java.io.FileNotFoundException

class CarburoidApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val locationService by lazy { LocationService(this) }
    lateinit var repository: GasStationRepository
        private set
    val cacheFile: File
        get() {
            val countryCode = CountryRegistry.current.countryCode
            return File(filesDir, "gas_stations_cache_${countryCode.lowercase()}.json")
        }

    override fun onCreate() {
        super.onCreate()
        LanguageSettings.initialize(this)
        ThemeSettings.initialize(this)
        CountrySettings.initialize(this)
        setupDebugData()
        repository = setupRepository()
        val context = this
        appScope.launch(Dispatchers.IO) {
            FranceExtraStationData.load(context)
        }
    }

    fun setupDebugData() {
        if (cacheFile.exists()) return
        val mockJson =
            try {
                val countryCode = CountryRegistry.current.countryCode
                assets.open("stations-debug-${countryCode.lowercase()}.json").bufferedReader().use { it.readText() }
            } catch (e: FileNotFoundException) {
                log("MOCK DATA NOT FOUND. RELEASE?")
                return
            }
        log("USING MOCK DATA. DEBUG?")
        cacheFile.writeText(mockJson)
    }

    fun setupRepository(): GasStationRepository {
        val repository =
            GasStationRepository(
                // TODO: This is convoluted, we might want to rethink how to inject the api
                api =
                    object : GasStationApi {
                        override suspend fun getGasStations() = CountryRegistry.current.api.getGasStations()
                    },
                parser = { json -> CountryRegistry.current.parse(json) },
                cacheFile = cacheFile,
                scope = appScope,
            )
        return repository
    }
}
