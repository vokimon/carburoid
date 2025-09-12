package net.canvoki.carburoid.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import android.content.Context
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
    private val context: Context? = null,
) {

    private val _events = MutableSharedFlow<RepositoryEvent>(replay = 0)
    val events: SharedFlow<RepositoryEvent> = _events.asSharedFlow()

    private var cache = GasStationResponse(emptyList())

    private var isBackgroundUpdateRunning = false

    suspend fun getStations(): List<GasStation> = emptyList()

    fun triggerBackgroundUpdate() {
        // TODO: Implement
    }

    suspend fun saveToCache(response: GasStationResponse) {
        cache = response
    }

    suspend fun clearCache() {
        // TODO: Implement
    }

    suspend fun getCache(): GasStationResponse = cache
}
