package net.canvoki.carburoid.repository

import java.util.concurrent.atomic.AtomicBoolean
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.model.GasStationResponse
import net.canvoki.carburoid.network.GasStationApi

typealias Parser = (String) -> GasStationResponse

sealed class RepositoryEvent {
    object UpdateStarted : RepositoryEvent()
    object UpdateReady : RepositoryEvent()
    data class UpdateFailed(val error: String) : RepositoryEvent()
}

class GasStationRepository(
    private val api: GasStationApi,
    private val cacheFile: File,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val parser: Parser? = null,
) {
    private val _events = MutableSharedFlow<RepositoryEvent>(replay = 0)
    val events: SharedFlow<RepositoryEvent> = _events.asSharedFlow()

    private var cache: String? = null
    private var parsed: GasStationResponse? = null

    init {
        if (cacheFile.exists()) {
            try {
                cache = cacheFile.readText()
                cache?.let {
                    parsed = parser?.invoke(it)
                }
            }
            catch(e: Exception) {
                cache = null
                parsed = null
                cacheFile.delete()
            }
        }
    }

    private val isBackgroundUpdateRunning = AtomicBoolean(false)

    suspend fun getStations(): List<GasStation>? = parsed?.stations

    suspend fun launchFetch() {
        if (!isBackgroundUpdateRunning.compareAndSet(false, true)) { // expected, new value
            return
        }
        scope.launch {
            _events.emit(RepositoryEvent.UpdateStarted)
            try {
                val response = api.getGasStations()
                if (parser != null)  {
                        parsed = parser(response)
                }
                saveToCache(response)
                _events.emit(RepositoryEvent.UpdateReady)
            }
            catch (e: Exception) {
                _events.emit(RepositoryEvent.UpdateFailed(e.message ?: e::class.simpleName ?: "Unknown"))
            }
            finally {
                isBackgroundUpdateRunning.set(false)
            }
        }
    }

    fun isFetchInProgress() = isBackgroundUpdateRunning.get()

    suspend fun saveToCache(response: String) {
        cache = response
        cacheFile.writeText(response)
    }

    suspend fun clearCache() {
        cache = null
        cacheFile.delete()
    }

    suspend fun getCache(): String? = cache
}
