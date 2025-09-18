package net.canvoki.carburoid.algorithms

import android.location.Location
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.distances.DistanceMethod
import net.canvoki.carburoid.distances.CurrentDistancePolicy
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.After
import org.junit.Test


// TODO: Ignore private gas stations
// TODO: Configurable product

/**
 * Distance is the absolute value of the longitude.
 * We cannot use a Location based solution since
 * this is an Android Framework not available for testing.
 */
class DummyDistanceMethod() : DistanceMethod {

    override fun computeDistance(station: GasStation): Float? {
        return station.longitude?.let {
            kotlin.math.abs(it).toFloat()
        }
    }

    override fun getReferenceName(): String = "Dummy"
}


fun dummyStation(index: Int, distance: Double, price: Double?): GasStation {
    return GasStation(
        name = "Station ${index} at ${distance} km, ${price} €",
        address = "Address $index",
        city = "A city",
        state = "A state",
        prices = mapOf(
            "Gasoleo A" to price,
        ),
        latitude = 40.4168,
        longitude = distance,
    )
}

fun assertResult(expected: List<String>, result: List<GasStation>) {
    //assertEquals(expected.joinToString("\n"), result.map { it.name!!}.joinToString("\n"))
    assertEquals(expected, result.map { it.name!!})
}


class StationFilterTest {

    private val filter = StationFilter()

    private var originalMethod: DistanceMethod? = null

    @Before
    fun saveOriginalMethod() {
        originalMethod = CurrentDistancePolicy.getMethod()
    }

    @After
    fun restoreOriginalMethod() {
        CurrentDistancePolicy.setMethod(originalMethod)
    }

    fun setDistancePolicy() {
        CurrentDistancePolicy.setMethod(DummyDistanceMethod())
    }

    fun testCase(stations: List<GasStation>, expected: List<String>) {
        setDistancePolicy()
        val result = filter.filter(stations)
        assertResult(expected, result)
    }

    @Test
    fun `empty list, returns empty list`() {
        testCase(
            stations = emptyList(),
            expected = emptyList(),
        )
    }

    @Test
    fun `single station, returns it`() {
        testCase(
            stations = listOf(
                dummyStation(index=1, distance=10.0, price=0.3),
            ),
            expected = listOf(
                "Station 1 at 10.0 km, 0.3 €",
            ),
        )
    }

    @Test
    fun `three stations, sorted by distance`() {
        testCase(
            stations = listOf(
                dummyStation(index=1, distance=20.0, price=0.3),
                dummyStation(index=2, distance=10.0, price=0.3), // This one is out of order
                dummyStation(index=3, distance=30.0, price=0.3),
            ),
            expected = listOf(
                "Station 2 at 10.0 km, 0.3 €",
                "Station 1 at 20.0 km, 0.3 €",
                "Station 3 at 30.0 km, 0.3 €",
            ),
        )
    }

    @Test
    fun `ignore further and expensive stations`() {
        testCase(
            stations = listOf(
                dummyStation(index=1, distance=20.0, price=0.3),
                dummyStation(index=2, distance=10.0, price=0.3),
                dummyStation(index=3, distance=15.0, price=0.4), // This one is further and expensive
            ),
            expected = listOf(
                "Station 2 at 10.0 km, 0.3 €",
                "Station 1 at 20.0 km, 0.3 €",
            ),
        )
    }

    @Test
    fun `ignore stations without the product`() {
        testCase(
            stations = listOf(
                dummyStation(index=1, distance=20.0, price=0.3),
                dummyStation(index=2, distance=10.0, price=0.3),
                dummyStation(index=3, distance=30.0, price=null), // This one has no price for the product
            ),
            expected = listOf(
                "Station 2 at 10.0 km, 0.3 €",
                "Station 1 at 20.0 km, 0.3 €",
            ),
        )
    }
}
