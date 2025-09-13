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

sealed class RepositoryEvent {
    object UpdateStarted : RepositoryEvent()
    object UpdateReady : RepositoryEvent()
    data class UpdateFailed(val error: String) : RepositoryEvent()
}

class GasStationRepository(
    private val api: GasStationApi,
    private val cacheFile: File,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {
    private val _events = MutableSharedFlow<RepositoryEvent>(replay = 0)
    val events: SharedFlow<RepositoryEvent> = _events.asSharedFlow()

    private var cache: String? = if (cacheFile.exists()) cacheFile.readText() else null

    private var isBackgroundUpdateRunning = false

    suspend fun getStations(): List<GasStation> = emptyList()

    suspend fun launchFetch() {
        isBackgroundUpdateRunning = true
        scope.launch {
            _events.emit(RepositoryEvent.UpdateStarted)
        }
        // val response = api.getGasStations()
    }

    fun isFetchInProgress() = isBackgroundUpdateRunning

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
