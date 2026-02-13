package net.canvoki.carburoid.repository

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.canvoki.carburoid.json.toSpanishFloat
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.model.GasStationResponse
import net.canvoki.carburoid.model.SpanishGasStation
import net.canvoki.carburoid.model.SpanishGasStationResponse
import net.canvoki.carburoid.network.GasStationApi
import net.canvoki.shared.test.assertEquals
import net.canvoki.shared.test.deferredCalls
import net.canvoki.shared.test.yieldUntilIdle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

class GasStationRepositoryTest {
    private lateinit var tempDir: File
    private lateinit var api: GasStationApi
    private lateinit var cacheFile: File

    suspend fun setupStations(stations: List<JsonObject>) {
        val response = jsonResponse(stations = stations)
        cacheFile.writeText(response)
    }

    fun jsonResponse(stations: List<JsonObject> = emptyList()): String =
        buildJsonObject {
            put(
                "ListaEESSPrecio",
                buildJsonArray {
                    stations.forEach { add(it) }
                },
            )
        }.toString()

    fun baseCacheContent(price: Double = 0.4) =
        jsonResponse(
            stations =
                listOf(
                    jsonStation(index = 1, distance = 10.0, price = price),
                ),
        )

    fun jsonStation(
        index: Int,
        distance: Double,
        price: Double?,
    ): JsonObject =
        buildJsonObject {
            put("IDEESS", "$index")
            put("Rótulo", "Station $index at $distance km, $price €")
            put("Dirección", "Address $index")
            put("Localidad", "A city")
            put("Provincia", "A state")
            put("Precio Gasoleo A", (toSpanishFloat(price) ?: ""))
            put("Latitud", "40,4168")
            put("Longitud (WGS84)", (toSpanishFloat(distance) ?: ""))
            put("Tipo Venta", "P")
        }

    private fun writeCache(downloadDate: Instant?) {
        val response =
            SpanishGasStationResponse(
                stations = emptyList(),
            )
        cacheFile.writeText(response.toJson())

        if (downloadDate != null) {
            cacheFile.setLastModified(downloadDate.toEpochMilli())
        }
    }

    private fun dataExample() =
        SpanishGasStationResponse(
            stations =
                listOf(
                    SpanishGasStation(
                        id = 666,
                        name = "Station 1",
                        address = "Address 1",
                        city = "City",
                        state = "State",
                        prices =
                            mapOf(
                                "Gasoleo A" to 1.5,
                            ),
                        latitude = 40.0,
                        longitude = -3.0,
                        isPublicPrice = true,
                    ),
                ),
        )

    private fun createRepository(
        coroutineContext: CoroutineContext,
        parser: Parser? = null,
    ): GasStationRepository {
        val scope = CoroutineScope(coroutineContext)
        val dispatcher = coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
        return GasStationRepository(
            api = api,
            cacheFile = cacheFile,
            parser = parser ?: { dataExample() },
            scope = scope,
            ioDispatcher = dispatcher,
        )
    }

    private suspend fun TestScope.collectEvents(
        repository: GasStationRepository,
        block: suspend () -> Unit,
    ): List<RepositoryEvent> {
        val events = mutableListOf<RepositoryEvent>()
        val collector =
            launch {
                repository.events.collect { events.add(it) }
            }
        yieldUntilIdle()

        try {
            block()
            yieldUntilIdle()
            return events
        } finally {
            collector.cancel()
        }
    }

    @Before
    fun setUp() {
        tempDir =
            Files.createTempDirectory("carburoid-test").toFile().apply {
                deleteOnExit()
            }

        api = mockk()
        cacheFile = File(tempDir, "test_cache.json")
    }

    @Test
    fun `getCache initially empty`() =
        runTest {
            val repository = createRepository(coroutineContext)
            assertNull(repository.getCache())
        }

    @Test
    fun `getCache after setting it`() =
        runTest {
            val repository = createRepository(coroutineContext)
            val response = baseCacheContent()
            repository.saveToCache(response)

            assertEquals(response, repository.getCache())
        }

    @Test
    fun `getCache after clear`() =
        runTest {
            val repository = createRepository(coroutineContext)
            val response = baseCacheContent()
            repository.saveToCache(response)
            repository.clearCache()

            assertNull(repository.getCache())
        }

    @Test
    fun `cache content survives repository recreation`() =
        runTest {
            val repository = createRepository(coroutineContext)
            val response = baseCacheContent(price = 0.3)
            repository.saveToCache(response)
            yieldUntilIdle()

            val repository2 = createRepository(coroutineContext)
            yieldUntilIdle()
            assertEquals(response, repository2.getCache())
        }

    @Test
    fun `cache clearing survives repository recreation`() =
        runTest {
            val repository = createRepository(coroutineContext)
            val response = baseCacheContent(price = 0.3)
            repository.saveToCache(response)
            repository.clearCache()

            val repository2 = GasStationRepository(api, cacheFile)
            yieldUntilIdle()
            assertNull(repository2.getCache())
        }

    @Test
    fun `launchFetch sets flag`() =
        runTest {
            val (deferred) = deferredCalls({ api.getGasStations() }, 1)
            val repository = createRepository(coroutineContext)

            repository.launchFetch()
            yieldUntilIdle()

            assertTrue(
                "isFetchInProgress() should return True",
                repository.isFetchInProgress(),
            )

            deferred.complete("Value")
        }

    @Test
    fun `launchFetch emits UpdateStarted when called`() =
        runTest {
            val (deferred) = deferredCalls({ api.getGasStations() }, 1)
            val repository = createRepository(coroutineContext)

            val events =
                collectEvents(repository) {
                    repository.launchFetch()
                }

            assertEquals(listOf(RepositoryEvent.UpdateStarted), events)
            deferred.complete("Value")
        }

    @Test
    fun `repository with no cache emits no events on init`() =
        runTest {
            assertFalse(cacheFile.exists())

            val repository = createRepository(coroutineContext)
            val events = collectEvents(repository) { }

            assertEquals(emptyList<RepositoryEvent>(), events)
        }

    @Test
    fun `launchFetch api called`() =
        runTest {
            val (deferred) = deferredCalls({ api.getGasStations() }, 1)
            val repository = createRepository(coroutineContext)

            // We don't care about events in this test — but we must collect them to avoid suspending forever
            coVerify(exactly = 0) { api.getGasStations() }

            repository.launchFetch()
            yieldUntilIdle()

            coVerify(exactly = 1) { api.getGasStations() }

            deferred.complete("Value")
            yieldUntilIdle()
        }

    @Test
    fun `launchFetch on success, flag unset and sets cache`() =
        runTest {
            val (deferred) = deferredCalls({ api.getGasStations() }, 1)
            val repository = createRepository(coroutineContext)

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
    fun `launchFetch on failure, flag unset and no cache`() =
        runTest {
            val (deferred) = deferredCalls({ api.getGasStations() }, 1)
            val repository = createRepository(coroutineContext)

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
    fun `launchFetch on failure, flag unset and cache kept`() =
        runTest {
            val (deferred) = deferredCalls({ api.getGasStations() }, 1)
            val repository = createRepository(coroutineContext)
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
    fun `launchFetch emits UpdateReady on success`() =
        runTest {
            val (deferred) = deferredCalls({ api.getGasStations() }, 1)
            val repository = createRepository(coroutineContext)

            repository.launchFetch()

            val events =
                collectEvents(repository) {
                    deferred.complete("Fetched Value")
                }

            assertEquals(listOf(RepositoryEvent.UpdateReady), events)
        }

    @Test
    fun `launchFetch emits UpdateFailed on failure`() =
        runTest {
            val (deferred) = deferredCalls({ api.getGasStations() }, 1)
            val repository = createRepository(coroutineContext)

            repository.launchFetch()
            val events =
                collectEvents(repository) {
                    deferred.completeExceptionally(RuntimeException("Emulated error"))
                }

            assertEquals(listOf(RepositoryEvent.UpdateFailed("Emulated error")), events)
        }

    @Test
    fun `launchFetch api skipped if pending api`() =
        runTest {
            val (deferred1, deferred2) = deferredCalls({ api.getGasStations() }, 2)
            val repository = createRepository(coroutineContext)

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
    fun `launchFetch api called if previous finished, is ok`() =
        runTest {
            val (deferred1, deferred2) = deferredCalls({ api.getGasStations() }, 2)
            val repository = createRepository(coroutineContext)

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
    fun `launchFetch fails if the serialization fails`() =
        runTest {
            val parser: Parser = { json ->
                throw Exception("Invalid JSON")
            }
            val repository = createRepository(coroutineContext, parser = parser)

            val (deferred) = deferredCalls({ api.getGasStations() }, 1)
            repository.launchFetch()
            val events =
                collectEvents(repository) {
                    deferred.complete("""Fetched content""")
                }

            assertEquals(listOf(RepositoryEvent.UpdateFailed("Invalid JSON")), events)
            assertNull(repository.getCache())
            assertFalse(repository.isFetchInProgress())
            assertEquals(null, repository.getData())
        }

    @Test
    fun `launchFetch updates data if the serialization works`() =
        runTest {
            val data = dataExample()
            val repository = createRepository(coroutineContext, parser = { json -> data })

            val (deferred) = deferredCalls({ api.getGasStations() }, 1)
            val eventsStart =
                collectEvents(repository) {
                    repository.launchFetch()
                }

            assertEquals(listOf(RepositoryEvent.UpdateStarted), eventsStart)

            val events =
                collectEvents(repository) {
                    deferred.complete("Fetched content")
                }
            assertEquals(listOf(RepositoryEvent.UpdateReady), events)

            assertEquals("Fetched content", repository.getCache())
            assertFalse(repository.isFetchInProgress())
            assertEquals(data, repository.getData())
        }

    @Test
    fun `before any fetch, getData returns null`() =
        runTest {
            val repository = createRepository(coroutineContext)

            val stations = repository.getData()

            assertEquals(null, stations)
        }

    @Test
    fun `parse cache on init`() =
        runTest {
            val data = dataExample()
            cacheFile.writeText("whatever")
            val repository = createRepository(coroutineContext, parser = { json -> data })

            yieldUntilIdle()

            assertEquals(data, repository.getData())
            assertEquals("whatever", repository.getCache())
        }

    fun createDeferredParser(): Pair<CompletableDeferred<GasStationResponse>, Parser> {
        val deferred = CompletableDeferred<GasStationResponse>()
        val parser: Parser = { _ -> deferred.await() }
        return deferred to parser
    }

    @Test
    fun `init with valid cache emits UpdateReady`() =
        runTest {
            val response = baseCacheContent()
            cacheFile.writeText(response)
            val (deferred, parser) = createDeferredParser()

            val repository = createRepository(coroutineContext, parser = parser)

            val events =
                collectEvents(repository) {
                    deferred.complete(dataExample())
                }

            assertEquals(listOf(RepositoryEvent.UpdateReady), events)
        }

    @Test
    fun `init with failed cache emits UpdateFailed and deletes cache file`() =
        runTest {
            cacheFile.writeText("whatever")

            val (deferred, parser) = createDeferredParser()
            val repository = createRepository(coroutineContext, parser = parser)

            val events =
                collectEvents(repository) {
                    deferred.completeExceptionally(Exception("An error"))
                }

            assertEquals(listOf(RepositoryEvent.UpdateFailed("An error")), events)
            assertEquals(null, repository.getCache())
            assertEquals(null, repository.getData())
            assertEquals(
                false,
                cacheFile.exists(),
                "Cache file should have been deleted",
            )
        }

    @Test
    fun `isExpired, true if missing cache`() =
        runTest {
            val repository = createRepository(coroutineContext)
            yieldUntilIdle()

            assertEquals(true, repository.isExpired())
        }

    @Test
    fun `isExpired, false if recent cache`() =
        runTest {
            writeCache(Instant.now())
            val repository = createRepository(coroutineContext)
            yieldUntilIdle()

            assertEquals(false, repository.isExpired())
        }

    @Test
    fun `isExpired, true if old cache`() =
        runTest {
            writeCache(Instant.now().minus(Duration.ofMinutes(GasStationRepository.MINUTES_TO_EXPIRE + 1)))
            val repository = createRepository(coroutineContext)
            yieldUntilIdle()

            assertEquals(true, repository.isExpired())
        }

    @Test
    fun `getStationById with a match`() =
        runTest {
            setupStations(
                listOf(
                    jsonStation(index = 1, distance = 10.0, price = 0.3),
                    jsonStation(index = 2, distance = 20.0, price = 0.3),
                ),
            )

            val repository =
                createRepository(coroutineContext, parser = { json -> SpanishGasStationResponse.parse(json) })
            yieldUntilIdle()
            var station = repository.getStationById(2)
            assertEquals(2, station?.id)
        }

    @Test
    fun `getStationById with no match`() =
        runTest {
            setupStations(
                listOf(
                    jsonStation(index = 1, distance = 10.0, price = 0.3),
                    jsonStation(index = 2, distance = 20.0, price = 0.3),
                ),
            )

            val repository =
                createRepository(coroutineContext, parser = { json -> SpanishGasStationResponse.parse(json) })
            yieldUntilIdle()
            var station = repository.getStationById(3)
            assertEquals(null, station)
        }

    @Test
    fun `getStationById when empty`() =
        runTest {
            val repository =
                createRepository(coroutineContext, parser = { json ->
                    SpanishGasStationResponse.parse(json)
                })
            yieldUntilIdle()
            var station = repository.getStationById(2)
            assertEquals(null, station)
        }

    @Test
    fun `setCache to non-existent file emits ready events`() =
        runTest {
            val repository = createRepository(coroutineContext)
            yieldUntilIdle()

            // Set cache to non-existent file
            val nonExistentFile = File(tempDir, "nonexistent.json")
            val events =
                collectEvents(repository) {
                    repository.setCache(nonExistentFile)
                    yieldUntilIdle()
                }
            // Should emit NO events
            assertEquals(listOf(RepositoryEvent.UpdateReady), events)
        }

    @Test
    fun `setCache with valid file loads successfully`() =
        runTest {
            // Setup valid cache
            val response = baseCacheContent()
            cacheFile.writeText(response)

            val repository = createRepository(coroutineContext)
            yieldUntilIdle() // Wait for initial load

            // Change to same file (should reload)
            repository.setCache(cacheFile)
            yieldUntilIdle()

            assertEquals(response, repository.getCache())
        }

    @Test
    fun `setCache with valid file emits UpdateReady`() =
        runTest {
            val repository = createRepository(coroutineContext)
            yieldUntilIdle()

            val response = baseCacheContent()
            val newCacheFile = File(tempDir, "new_cache.json")
            newCacheFile.writeText(response)

            val events =
                collectEvents(repository) {
                    repository.setCache(newCacheFile)
                }

            assertEquals(
                listOf(
                    RepositoryEvent.UpdateStarted,
                    RepositoryEvent.UpdateReady,
                ),
                events,
            )
        }

    @Test
    fun `setCache with invalid file fails and deletes file`() =
        runTest {
            val repository = createRepository(coroutineContext, parser = { throw Exception("Parse error") })
            yieldUntilIdle()

            // Verify initial state
            assertNull(repository.getCache())
            assertFalse(cacheFile.exists())

            // Set cache to new invalid file
            val newCacheFile = File(tempDir, "new_cache.json")
            newCacheFile.writeText("also invalid")
            repository.setCache(newCacheFile)
            yieldUntilIdle()

            assertNull(repository.getCache())
            assertFalse(newCacheFile.exists())
        }

    @Test
    fun `setCache cancels ongoing fetch`() =
        runTest {
            val (deferred) = deferredCalls({ api.getGasStations() }, 1)
            val repository = createRepository(coroutineContext)

            // Start a fetch
            repository.launchFetch()
            yieldUntilIdle()
            assertTrue(repository.isFetchInProgress())

            // Set new cache file - should cancel fetch
            val newCacheFile = File(tempDir, "new_cache.json")
            newCacheFile.writeText("new cache content")
            repository.setCache(newCacheFile)
            yieldUntilIdle()

            // Fetch should be cancelled, cache should be loaded
            assertFalse(repository.isFetchInProgress())
            assertEquals(repository.getCache(), "new cache content")

            // Complete the original deferred (should be ignored)
            deferred.complete("old value")
            yieldUntilIdle()
            assertEquals(repository.getCache(), "new cache content") // Should still have new cache
        }

    @Test
    fun `launchFetch skipped during setCache initialization`() =
        runTest {
            val (deferred) = deferredCalls({ api.getGasStations() }, 1)
            val repository = createRepository(coroutineContext)

            // Set cache to trigger initialization
            val newCacheFile = File(tempDir, "new_cache.json")
            newCacheFile.writeText(baseCacheContent())
            repository.setCache(newCacheFile)

            // Immediately try to launch fetch - should be skipped
            repository.launchFetch()
            yieldUntilIdle()

            // Only cache load should have happened, no API call
            coVerify(exactly = 0) { api.getGasStations() }

            // Complete any pending operations
            deferred.complete("value")
            yieldUntilIdle()
        }
}
