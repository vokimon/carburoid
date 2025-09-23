package net.canvoki.carburoid.repository

import com.google.gson.Gson
import java.util.concurrent.atomic.AtomicBoolean
import java.io.File
import java.time.Instant
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import net.canvoki.carburoid.*
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
    private val parser: Parser? = { json ->
        GasStationResponse.parse(json)
    },
) {
    companion object {
        const val minutesToExpire = 30L
    }
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
                    log("REUSING PREVIOUS CACHE")
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

    fun getData(): GasStationResponse? {
        if (isExpired()) {
            launchFetch()
        }
        return parsed
    }

    fun launchFetch() {
        if (!isBackgroundUpdateRunning.compareAndSet(false, true)) { // expected, new value
            log("ALREADY FETCHING, QUIT")
            return
        }
        scope.launch {
            _events.emit(RepositoryEvent.UpdateStarted)
            try {
                log("STARTING FETCH")
                val response =  api.getGasStations()

                if (parser != null)  {

                    parsed = timeit("PARSING FETCH") {
                        parser(response)
                    }
                }
                saveToCache(response)
                _events.emit(RepositoryEvent.UpdateReady)
            }
            catch (e: Exception) {
                val message = e.message ?: e::class.simpleName ?: "Unknown"
                log("FETCH ERROR $message")
                _events.emit(RepositoryEvent.UpdateFailed(message))
            }
            finally {
                isBackgroundUpdateRunning.set(false)
            }
        }
    }

    fun isFetchInProgress() = isBackgroundUpdateRunning.get()

    fun isExpired() : Boolean {
        val p = parsed ?: return true
        val date = p.downloadDate ?: return true

        val deadline = Instant.now().minus(Duration.ofMinutes(minutesToExpire))
        if (date <= deadline) return true
        //up-to-date cache
        return false
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
