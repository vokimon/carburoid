package net.canvoki.carburoid.repository

import com.google.gson.Gson
import io.mockk.mockk
import io.mockk.coEvery
import io.mockk.coVerify
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.time.Duration
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import net.canvoki.carburoid.json.toSpanishFloat
import net.canvoki.carburoid.test.yieldUntilIdle
import net.canvoki.carburoid.test.deferredCalls
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.model.GasStationResponse
import net.canvoki.carburoid.network.GasStationApi


class GasStationRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var api: GasStationApi
    private lateinit var cacheFile: File
    private lateinit var repository: GasStationRepository

    fun jsonResponse(
        stations: List<Map<String, Any>> = emptyList()
    ): String {
        return Gson().toJson(
            mapOf("ListaEESSPrecio" to stations)
        )
    }

    fun station(index: Int, distance: Double, price: Double?): Map<String, Any> {
        return mapOf(
            "IDEESS" to "$index",
            "Rótulo" to "Station $index at $distance km, $price €",
            "Dirección" to "Address $index",
            "Localidad" to "A city",
            "Provincia" to "A state",
            "Precio Gasoleo A" to (toSpanishFloat(price) ?: ""),
            "Latitud" to "40,4168",
            "Longitud (WGS84)" to (toSpanishFloat(distance) ?: ""),
            "Tipo Venta" to "P",
        )
    }

    private fun dataExample() = GasStationResponse(
        stations=listOf(
            GasStation(
                id=666,
                name="Station 1",
                address="Address 1",
                city="City",
                state="State",
                prices=mapOf(
                    "Gasoleo A" to 1.5,
                ),
                latitude=40.0,
                longitude=-3.0,
                isPublicPrice=true,
            ),
        )
    )

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("carburoid-test").toFile().apply {
            deleteOnExit()
        }

        api = mockk()
        cacheFile = File(tempDir, "test_cache.json")
        repository = GasStationRepository(api, cacheFile, parser=null)
    }

    @Test
    fun `getCache initially empty`() = runTest {
        assertNull(repository.getCache())
    }

    @Test
    fun `getCache after setting it`() = runTest {
        val response = jsonResponse(stations=listOf(
            station(index=1, distance=10.0, price=0.3),
        ))
        repository.saveToCache(response)

        assertEquals(response, repository.getCache())
    }

    @Test
    fun `getCache after clear`() = runTest {
        val response = jsonResponse(stations=listOf(
            station(index=1, distance=10.0, price=0.3),
        ))
        repository.saveToCache(response)
        repository.clearCache()

        assertNull(repository.getCache())
    }

    @Test
    fun `cache content survives repository recreation`() = runTest {
        val response = jsonResponse(listOf(
            station(index = 1, distance = 10.0, price = 0.3)
        ))
        repository.saveToCache(response)

        val repository2 = GasStationRepository(api, cacheFile, parser=null)
        assertEquals(response, repository2.getCache())
    }

    @Test
    fun `cache clearing survives repository recreation`() = runTest {
        val response = jsonResponse(listOf(
            station(index = 1, distance = 10.0, price = 0.3)
        ))
        repository.saveToCache(response)
        repository.clearCache()

        val repository2 = GasStationRepository(api, cacheFile)
        assertNull(repository2.getCache())
    }

    @Test
    fun `launchFetch sets flag`() = runTest {
        val (deferred) = deferredCalls({ api.getGasStations() }, 1)
        val repository = GasStationRepository(api, cacheFile, this)

        repository.launchFetch()
        yieldUntilIdle()

        assertTrue(
            "isFetchInProgress() should return True",
            repository.isFetchInProgress(),
        )

        deferred.complete("Value")
    }

    @Test
    fun `launchFetch emits UpdateStarted when called`() = runTest {
        val (deferred) = deferredCalls({ api.getGasStations() }, 1)
        val repository = GasStationRepository(api, cacheFile, scope=this, parser=null)

        val events = mutableListOf<RepositoryEvent>()
        val eventCollector = launch {
            repository.events.collect { events.add(it) }
        }
        yieldUntilIdle()  // Let collector start

        repository.launchFetch()
        yieldUntilIdle()

        assertEquals(listOf(RepositoryEvent.UpdateStarted), events)

        deferred.complete("Value")

        eventCollector.cancel()
    }

    @Test
    fun `launchFetch api called`() = runTest {
        val (deferred) = deferredCalls({ api.getGasStations() }, 1)
        val repository = GasStationRepository(api, cacheFile, scope=this, parser=null)

        // We don't care about events in this test — but we must collect them to avoid suspending forever
        coVerify(exactly = 0) { api.getGasStations() }

        repository.launchFetch()
        yieldUntilIdle()

        coVerify(exactly = 1) { api.getGasStations() }

        deferred.complete("Value")
        yieldUntilIdle()

    }

    @Test
    fun `launchFetch on success, flag unset and sets cache`() = runTest {
        val (deferred) = deferredCalls({ api.getGasStations() }, 1)
        val repository = GasStationRepository(api, cacheFile, scope=this, parser=null)

        repository.launchFetch()
        yieldUntilIdle()

        deferred.complete("Fetched Value")
        yieldUntilIdle()

        assertFalse(
            "isFetchInProgress() should return False",
            repository.isFetchInProgress(),
        )
        assertEquals("Fetched Value", repository.getCache())
    }

    @Test
    fun `launchFetch on failure, flag unset and no cache`() = runTest {
        val (deferred) = deferredCalls({ api.getGasStations() }, 1)
        val repository = GasStationRepository(api, cacheFile, scope=this, parser=null)

        repository.launchFetch()
        yieldUntilIdle()

        deferred.completeExceptionally(RuntimeException("Emulated error"))
        yieldUntilIdle()

        assertFalse(
            "isFetchInProgress() should return False",
            repository.isFetchInProgress(),
        )
        assertEquals(null, repository.getCache())
    }

    @Test
    fun `launchFetch on failure, flag unset and cache kept`() = runTest {
        val (deferred) = deferredCalls({ api.getGasStations() }, 1)
        val repository = GasStationRepository(api, cacheFile, scope=this, parser=null)
        repository.saveToCache("Previous value")

        repository.launchFetch()
        yieldUntilIdle()

        deferred.completeExceptionally(RuntimeException("Emulated error"))
        yieldUntilIdle()

        assertFalse(
            "isFetchInProgress() should return False",
            repository.isFetchInProgress(),
        )
        assertEquals("Previous value", repository.getCache())
    }

    @Test
    fun `launchFetch emits UpdateReady on success`() = runTest {
        val (deferred) = deferredCalls({ api.getGasStations() }, 1)
        val repository = GasStationRepository(api, cacheFile, scope=this, parser=null)

        val events = mutableListOf<RepositoryEvent>()
        val eventCollector = launch {
            repository.events.collect { events.add(it) }
        }
        yieldUntilIdle()  // Let collector start

        repository.launchFetch()
        yieldUntilIdle()

        deferred.complete("Fetched Value")
        yieldUntilIdle()

        assertEquals(listOf(
            RepositoryEvent.UpdateStarted,
            RepositoryEvent.UpdateReady,
        ), events)

        eventCollector.cancel()
    }

    @Test
    fun `launchFetch emits UpdateFailed on failure`() = runTest {
        val (deferred) = deferredCalls({ api.getGasStations() }, 1)
        val repository = GasStationRepository(api, cacheFile, scope=this, parser=null)

        val events = mutableListOf<RepositoryEvent>()
        val eventCollector = launch {
            repository.events.collect { events.add(it) }
        }
        yieldUntilIdle()  // Let collector start

        repository.launchFetch()
        yieldUntilIdle()

        deferred.completeExceptionally(RuntimeException("Emulated error"))
        yieldUntilIdle()

        assertEquals(listOf(
            RepositoryEvent.UpdateStarted,
            RepositoryEvent.UpdateFailed("Emulated error"),
        ), events)

        eventCollector.cancel()
    }

    @Test
    fun `launchFetch api skipped if pending api`() = runTest {
        val (deferred1, deferred2) = deferredCalls({ api.getGasStations() }, 2)
        val repository = GasStationRepository(api, cacheFile, scope=this, parser=null)

        // We don't care about events in this test — but we must collect them to avoid suspending forever
        coVerify(exactly = 0) { api.getGasStations() }

        repository.launchFetch()
        yieldUntilIdle()

        coVerify(exactly = 1) { api.getGasStations() }

        repository.launchFetch()
        yieldUntilIdle()

        coVerify(exactly = 1) { api.getGasStations() }

        deferred1.complete("Value1")
        deferred2.complete("Value2")
        yieldUntilIdle()

    }

    @Test
    fun `launchFetch api called if previous finished, is ok`() = runTest {
        val (deferred1, deferred2) = deferredCalls({ api.getGasStations() }, 2)
        val repository = GasStationRepository(api, cacheFile, scope=this, parser=null)

        // We don't care about events in this test — but we must collect them to avoid suspending forever
        coVerify(exactly = 0) { api.getGasStations() }

        repository.launchFetch()
        yieldUntilIdle()

        coVerify(exactly = 1) { api.getGasStations() }

        deferred1.complete("Value1")
        yieldUntilIdle()

        repository.launchFetch()
        yieldUntilIdle()

        coVerify(exactly = 2) { api.getGasStations() }

        deferred2.complete("Value2")
        yieldUntilIdle()
    }

    @Test
    fun `launchFetch fails if the serialization fails`() = runTest {
        val parser: Parser  = { json ->
            throw Exception("Invalid JSON")
        }
        val repository = GasStationRepository(
            api = api,
            cacheFile = cacheFile,
            parser = parser,
            scope = this,
        )

        val (deferred) = deferredCalls({ api.getGasStations() }, 1)
        val events = mutableListOf<RepositoryEvent>()
        val eventCollector = launch { repository.events.collect { events.add(it) } }
        yieldUntilIdle()

        repository.launchFetch()
        yieldUntilIdle()

        deferred.complete("""Fetched content""")
        yieldUntilIdle()

        assertEquals(
            listOf(
                RepositoryEvent.UpdateStarted,
                RepositoryEvent.UpdateFailed("Invalid JSON"),
            ),
            events
        )
        assertNull(repository.getCache())
        assertFalse(repository.isFetchInProgress())
        assertEquals(null, repository.getData())

        eventCollector.cancel()
    }

    @Test
    fun `launchFetch updates data if the serialization works`() = runTest {
        val data = dataExample()
        val repository = GasStationRepository(
            api = api,
            cacheFile = cacheFile,
            parser = { json -> data },
            scope = this,
        )

        val (deferred) = deferredCalls({ api.getGasStations() }, 1)
        val events = mutableListOf<RepositoryEvent>()
        val eventCollector = launch { repository.events.collect { events.add(it) } }
        yieldUntilIdle()

        repository.launchFetch()
        yieldUntilIdle()

        deferred.complete("Fetched content")
        yieldUntilIdle()

        assertEquals(
            listOf(
                RepositoryEvent.UpdateStarted,
                RepositoryEvent.UpdateReady,
            ),
            events
        )
        assertEquals("Fetched content", repository.getCache())
        assertFalse(repository.isFetchInProgress())
        assertEquals(data, repository.getData())

        eventCollector.cancel()
    }

    @Test
    fun `before any fetch, getData returns null`() = runTest {
        val repository = GasStationRepository(api, cacheFile, scope=this, parser=null)

        val stations = repository.getData()

        assertEquals(null, stations)
    }

    @Test
    fun `parse cache on init`() = runTest {
        val data = dataExample()
        cacheFile.writeText("whatever")

        val repository = GasStationRepository(
            api = api,
            cacheFile = cacheFile,
            parser = { json -> data },
            scope = this,
        )

        assertEquals(data, repository.getData())
        assertEquals("whatever", repository.getCache())
    }

    @Test
    fun `parse cache on init fails, at to null, cache file deleted`() = runTest {
        val data = dataExample()
        cacheFile.writeText("whatever")

        val repository = GasStationRepository(
            api = api,
            cacheFile = cacheFile,
            parser = { json -> throw Exception("An error") },
            scope = this,
        )

        assertEquals(null, repository.getCache())
        assertEquals(null, repository.getData())
        assertEquals(
            "Cache file should have been deleted",
            false, cacheFile.exists()
        )
    }

    private fun writeCache(downloadDate: Instant?, stations: List<GasStation>?=null) {
        val response = GasStationResponse(
            stations = stations?:emptyList(),
            downloadDate = downloadDate,
        )
        cacheFile.writeText(Gson().toJson(response))
    }

    @Test
    fun `isExpired, true if missing cache`() = runTest {
        val repository = GasStationRepository(api, cacheFile, scope=this, parser=null)

        assertEquals(true, repository.isExpired())
    }

    @Test
    fun `isExpired, false if recent cache`() = runTest {
        writeCache(Instant.now())
        val repository = GasStationRepository(api, cacheFile, this)

        assertEquals(false, repository.isExpired())
    }

    @Test
    fun `isExpired, true if unknown date`() = runTest {
        writeCache(null)
        val repository = GasStationRepository(api, cacheFile, this)

        assertEquals(true, repository.isExpired())
    }

    @Test
    fun `isExpired, true if old cache`() = runTest {
        writeCache(Instant.now().minus(Duration.ofMinutes(GasStationRepository.minutesToExpire)))
        val repository = GasStationRepository(api, cacheFile, this)

        assertEquals(true, repository.isExpired())
    }

    suspend fun setupStations(stations: List<Map<String,Any>>) {
        val response = jsonResponse(stations=stations)
        repository.saveToCache(response)
    }

    @Test
    fun `getStationById with a match`() = runTest {
        setupStations(listOf(
            station(index=1, distance=10.0, price=0.3),
            station(index=2, distance=20.0, price=0.3),
        ))

        val repository = GasStationRepository(api, cacheFile, this)
        var station = repository.getStationById(2)
        assertEquals(2, station?.id)
    }

    @Test
    fun `getStationById with no match`() = runTest {
        setupStations(listOf(
            station(index=1, distance=10.0, price=0.3),
            station(index=2, distance=20.0, price=0.3),
        ))

        val repository = GasStationRepository(api, cacheFile, this)
        var station = repository.getStationById(3)
        assertEquals(null, station)
    }

    @Test
    fun `getStationById when empty`() = runTest {
        val repository = GasStationRepository(api, cacheFile, this)

        var station = repository.getStationById(2)
        assertEquals(null, station)
    }
}
