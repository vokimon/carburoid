package net.canvoki.carburoid.repository

import com.google.gson.Gson
import io.mockk.mockk
import io.mockk.coEvery
import io.mockk.coVerify
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import net.canvoki.carburoid.test.yieldUntilIdle
import net.canvoki.carburoid.network.GasStationApi

/*
Cache

- Setup/Teardown: Use special testing database location, cleared before and after each tests
- Setup/Teardown: Mock api so that all calls fail. No yet called but in provision it will, we could add this later.
- TestCase: Create a Repo, assert stations is []
- TestCase: Create a Repo, set cache to data, assert cachedStations is filled
- TestCase: Create a Repo, set cache to data, Destroy the repo, Create a second Repo, assert cachedStations still filled because of serialization.

Then continue with the triggerBackgroundUpdate method in isolation without the middleman getStations()
Cases:

- Calling it calls the api, assert that triggers the event, and sets the flag
- Resolving the api to result, assert it triggers the event, unsets the flags, and cachedStations is updated
- Resolving the api to failure, assert it triggers the event, unsets the flag, and cachedStations is kept
*/

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
            "Rótulo" to "Station $index at $distance km, $price €",
            "Dirección" to "Address $index",
            "Localidad" to "A city",
            "Provincia" to "A state",
            "Precio Gasoleo A" to "${price?.toString()?.replace(".", ",") ?: ""}",
            "Latitud" to "40,4168",
            "Longitud (WGS84)" to "${distance.toString().replace(".", ",")}",
        )
    }

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("carburoid-test").toFile().apply {
            deleteOnExit()
        }

        api = mockk()
        cacheFile = File(tempDir, "test_cache.json")
        repository = GasStationRepository(api, cacheFile)
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

        val repository2 = GasStationRepository(api, cacheFile)
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
}
