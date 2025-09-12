package net.canvoki.carburoid.repository

import io.mockk.mockk
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import net.canvoki.carburoid.algorithms.DummyDistanceMethod
import net.canvoki.carburoid.algorithms.dummyStation
import net.canvoki.carburoid.algorithms.assertResult
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
    private lateinit var repository: GasStationRepository

    fun dummyResponse(
        stations: List<GasStation> = emptyList()
    ): GasStationResponse {
        return GasStationResponse(stations)
    }

    @Before
    fun setUp() {
        api = mockk()
        repository = GasStationRepository(api)
    }

    @Test
    fun `getCache initially empty`() = runTest {
        val response = dummyResponse()
        repository.saveToCache(response)

        val cached = repository.getCache()
        assertResult(
            emptyList(),
            cached.stations,
        )
    }

    @Test
    fun `getCache after setting it`() = runTest {
        val response = dummyResponse(listOf(
            dummyStation(index=1, distance=10.0, price=0.3),
        ))
        repository.saveToCache(response)

        val cached = repository.getCache()
        assertResult(
            listOf("Station 1 at 10.0 km, 0.3 €"),
            cached.stations,
        )
    }

    @Test
    fun `getCache after clear`() = runTest {
        val response = dummyResponse(listOf(
            dummyStation(index=1, distance=10.0, price=0.3),
        ))
        repository.saveToCache(response)
        repository.clearCache()

        val cached = repository.getCache()
        assertResult(
            emptyList(),
            cached.stations,
        )
    }

}
