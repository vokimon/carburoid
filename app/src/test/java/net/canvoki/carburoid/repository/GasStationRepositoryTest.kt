package net.canvoki.carburoid.repository

import com.google.gson.Gson
import io.mockk.mockk
import io.mockk.coEvery
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.model.GasStationResponse
import net.canvoki.carburoid.network.GasStationApi
import net.canvoki.carburoid.repository.GasStationRepository

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

    private lateinit var api: GasStationApi
    private lateinit var cacheFile: File
    private lateinit var repository: GasStationRepository


    fun jsonResponse(
        stations: List<Map<String, Any>> = emptyList()
    ): String {
        return Gson().toJson(
            mapOf(
                "ListaEESSPrecio" to stations
            )
        )
    }

    fun station(index: Int, distance: Double, price: Double?): Map<String, Any> {
        return mapOf(
            "Rótulo" to "Station ${index} at ${distance} km, ${price} €",
            "Dirección" to "Address $index",
            "Localidad" to "A city",
            "Provincia" to "A state",
            "Precio Gasoleo A" to "${price?.toString()?.replace(".", ",") ?: ""}",
            "Latitud" to "40,4168",
            "Longitud (WGS84)" to "${distance.toString().replace(".",",")}",
        )
    }

    @Before
    fun setUp() {
        api = mockk()
        cacheFile = File.createTempFile("test_cache", ".json")
        cacheFile.delete()
        repository = GasStationRepository(api, cacheFile)
    }
    @After
    fun tearDown() {
        cacheFile.delete()
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

        val repository2 = GasStationRepository(api, cacheFile)  // ✅ New instance

        // ✅ Then: second repository sees same data
        assertEquals(response, repository2.getCache())
    }

    @Test
    fun `cache clearing survives repository recreation`() = runTest {
        val response = jsonResponse(listOf(
            station(index = 1, distance = 10.0, price = 0.3)
        ))
        repository.saveToCache(response)
        repository.clearCache()

        val repository2 = GasStationRepository(api, cacheFile)  // ✅ New instance

        // ✅ Then: second repository sees same data
        assertNull(repository2.getCache())
    }
}
