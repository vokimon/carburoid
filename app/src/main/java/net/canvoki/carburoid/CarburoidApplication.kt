package net.canvoki.carburoid

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.canvoki.carburoid.country.CountryRegistry
import net.canvoki.carburoid.location.LocationService
import net.canvoki.carburoid.repository.GasStationRepository
import net.canvoki.carburoid.ui.settings.LanguageSettings
import net.canvoki.carburoid.ui.settings.ThemeSettings
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
        LanguageSettings.apply(this)
        ThemeSettings.apply(this)
        setupDebugData()
        repository = setupRepository()
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
        val country = CountryRegistry.current
        val repository =
            GasStationRepository(
                api = country.api,
                parser = { json -> country.parse(json) },
                cacheFile = cacheFile,
                scope = appScope,
            )
        return repository
    }
}
