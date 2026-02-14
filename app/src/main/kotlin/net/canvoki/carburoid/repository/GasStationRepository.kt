package net.canvoki.carburoid.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.model.GasStationResponse
import net.canvoki.carburoid.model.SpanishGasStationResponse
import net.canvoki.carburoid.network.GasStationApi
import net.canvoki.shared.timeit
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import net.canvoki.shared.nolog as log

typealias Parser = suspend (String) -> GasStationResponse

sealed class RepositoryEvent {
    object UpdateStarted : RepositoryEvent()

    object UpdateReady : RepositoryEvent()

    data class UpdateFailed(
        val error: String,
    ) : RepositoryEvent()
}

class GasStationRepository(
    private val api: GasStationApi,
    private var cacheFile: File,
    private val parser: Parser? = { json ->
        SpanishGasStationResponse.parse(json)
    },
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    companion object {
        const val MINUTES_TO_EXPIRE = 30L
    }

    private val _events = MutableSharedFlow<RepositoryEvent>(replay = 0)
    val events: SharedFlow<RepositoryEvent> = _events.asSharedFlow()

    private var cache: String? = null
    private var parsed: GasStationResponse? = null
    private var currentFetchJob: Job? = null

    init {
        updateFromCache()
    }

    private fun updateFromCache() {
        cache = null
        parsed = null
        if (!cacheFile.exists()) {
            scope.launch(ioDispatcher) {
                _events.emit(RepositoryEvent.UpdateReady)
            }
            return
        }

        val holder =
            object {
                var job: Job? = null
            }
        holder.job =
            scope.launch(ioDispatcher) {
                try {
                    _events.emit(RepositoryEvent.UpdateStarted)
                    coroutineContext.ensureActive()

                    log("LOADING PREVIOUS CACHE")
                    val response = cacheFile.readText()
                    //log("RESPONSE $response")
                    coroutineContext.ensureActive()

                    var success = false
                    val newParsed = timeit("PARSING") { parser?.invoke(response) }
                    coroutineContext.ensureActive()
                    if (newParsed != null) {
                        cache = response
                        parsed = newParsed
                        currentFetchJob = null
                        _events.emit(RepositoryEvent.UpdateReady)
                        success = true
                    }
                    if (!success && currentFetchJob == holder.job) {
                        log("ERROR LOADING PREVIOUS CACHE RETURNING NULL")
                        cache = null
                        parsed = null
                        cacheFile.delete()
                        currentFetchJob = null
                        _events.emit(RepositoryEvent.UpdateFailed("Error loading previous data"))
                    }
                } catch (e: CancellationException) {
                    log("CACHE LOAD CANCELLED")
                    if (currentFetchJob == holder.job) {
                        currentFetchJob = null
                    }
                } catch (e: Exception) {
                    log("ERROR LOADING PREVIOUS CACHE $e")
                    cache = null
                    parsed = null
                    cacheFile.delete()
                    currentFetchJob = null
                    val message = e.message ?: e::class.simpleName ?: "Unknown"
                    _events.emit(RepositoryEvent.UpdateFailed(message))
                }
            }

        currentFetchJob = holder.job
    }

    fun setCache(cacheFile: File) {
        log("RESETING CACHE: $cacheFile")
        this.cacheFile = cacheFile
        updateFromCache()
    }

    fun getData(): GasStationResponse? {
        if (isExpired()) {
            launchFetch()
        }
        return parsed
    }

    fun getStationById(id: Int): GasStation? = getData()?.stations?.find { it.id == id }

    fun cancelFetchIfInProgress() {
        currentFetchJob?.cancel()
    }

    fun launchFetch() {
        if (currentFetchJob?.isActive == true) {
            log("FETCH ALREADY IN PROGRESS, SKIPPING")
            return
        }
        // To safely refer to it from the coroutine
        val holder =
            object {
                var job: Job? = null
            }
        holder.job =
            scope.launch(ioDispatcher) {
                try {
                    _events.emit(RepositoryEvent.UpdateStarted)
                    coroutineContext.ensureActive()

                    val response = timeit("FETCH") { api.getGasStations() }
                    coroutineContext.ensureActive()

                    val newParsed = timeit("PARSING") { parser?.invoke(response) }
                    coroutineContext.ensureActive()

                    if (currentFetchJob == holder.job) {
                        saveToCache(response)
                        cache = response
                        parsed = newParsed
                        currentFetchJob = null
                        _events.emit(RepositoryEvent.UpdateReady)
                    }
                } catch (e: CancellationException) {
                    log("FETCH CANCELLED")
                    if (currentFetchJob == holder.job) {
                        currentFetchJob = null
                    }
                } catch (e: Exception) {
                    val message = e.message ?: e::class.simpleName ?: "Unknown"
                    log("FETCH ERROR: $message")
                    if (currentFetchJob == holder.job) {
                        currentFetchJob = null
                        _events.emit(RepositoryEvent.UpdateFailed(message))
                    }
                }
            }

        currentFetchJob = holder.job
    }

    fun isFetchInProgress(): Boolean = currentFetchJob?.isActive == true

    fun isExpired(): Boolean {
        val p = parsed ?: return true
        val cacheInstant = Instant.ofEpochMilli(cacheFile.lastModified())
        val deadline = Instant.now().minus(Duration.ofMinutes(MINUTES_TO_EXPIRE))
        return cacheInstant <= deadline
    }

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
