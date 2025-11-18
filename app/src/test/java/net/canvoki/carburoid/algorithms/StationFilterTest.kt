package net.canvoki.carburoid.algorithms

import net.canvoki.carburoid.distances.CurrentDistancePolicy
import net.canvoki.carburoid.distances.DistanceMethod
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.model.OpeningHours
import net.canvoki.carburoid.test.atMadridInstant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

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

fun dummyStation(
    index: Int,
    distance: Double,
    price: Double?,
    isPublicPrice: Boolean = true,
    hours: String = "L-D: 24H",
): GasStation {
    return GasStation(
        id = index,
        name = "Station $index at $distance km, $price €",
        address = "Address $index",
        city = "A city",
        state = "A state",
        prices =
            mapOf(
                "Gasoleo A" to price,
            ),
        latitude = 40.4168,
        longitude = distance / 100.0,
        isPublicPrice = isPublicPrice,
        openingHours = OpeningHours.parse(hours) ?: OpeningHours(),
    )
}

fun assertResult(
    expected: List<String>,
    result: List<GasStation>,
) {
    assertEquals(expected.joinToString("\n"), result.map { it.name!! }.joinToString("\n"))
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

    fun testCase(
        stations: List<GasStation>,
        expected: List<String>,
        hideExpensiveFurther: Boolean = true,
        onlyPublicPrices: Boolean = true,
        hideClosedMarginInMinutes: Int = 0,
    ) {
        setDistancePolicy()
        val config =
            FilterConfig(
                hideExpensiveFurther = hideExpensiveFurther,
                onlyPublicPrices = onlyPublicPrices,
                hideClosedMarginInMinutes = hideClosedMarginInMinutes,
            )
        val result = StationFilter(config).filter(stations)
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
            stations =
                listOf(
                    dummyStation(index = 1, distance = 10.0, price = 0.3),
                ),
            expected =
                listOf(
                    "Station 1 at 10.0 km, 0.3 €",
                ),
        )
    }

    @Test
    fun `three stations, sorted by distance`() {
        testCase(
            stations =
                listOf(
                    dummyStation(index = 1, distance = 20.0, price = 0.3),
                    dummyStation(index = 2, distance = 10.0, price = 0.3), // This one is out of order
                    dummyStation(index = 3, distance = 30.0, price = 0.3),
                ),
            expected =
                listOf(
                    "Station 2 at 10.0 km, 0.3 €",
                    "Station 1 at 20.0 km, 0.3 €",
                    "Station 3 at 30.0 km, 0.3 €",
                ),
        )
    }

    @Test
    fun `ignore further and expensive stations`() {
        testCase(
            stations =
                listOf(
                    dummyStation(index = 1, distance = 20.0, price = 0.3),
                    dummyStation(index = 2, distance = 10.0, price = 0.3),
                    dummyStation(index = 3, distance = 15.0, price = 0.4), // This one is further and expensive
                ),
            expected =
                listOf(
                    "Station 2 at 10.0 km, 0.3 €",
                    "Station 1 at 20.0 km, 0.3 €",
                ),
        )
    }

    @Test
    fun `do not ignore further and expensive stations if configured`() {
        testCase(
            hideExpensiveFurther = false,
            stations =
                listOf(
                    dummyStation(index = 1, distance = 20.0, price = 0.3),
                    dummyStation(index = 2, distance = 10.0, price = 0.3),
                    dummyStation(index = 3, distance = 15.0, price = 0.4), // This one is further and expensive
                ),
            expected =
                listOf(
                    "Station 2 at 10.0 km, 0.3 €",
                    "Station 3 at 15.0 km, 0.4 €",
                    "Station 1 at 20.0 km, 0.3 €",
                ),
        )
    }

    @Test
    fun `ignore stations without the product`() {
        testCase(
            stations =
                listOf(
                    dummyStation(index = 1, distance = 20.0, price = 0.3),
                    dummyStation(index = 2, distance = 10.0, price = 0.3),
                    dummyStation(index = 3, distance = 30.0, price = null), // This one has no price for the product
                ),
            expected =
                listOf(
                    "Station 2 at 10.0 km, 0.3 €",
                    "Station 1 at 20.0 km, 0.3 €",
                ),
        )
    }

    @Test
    fun `filter non public price`() {
        testCase(
            stations =
                listOf(
                    dummyStation(index = 1, distance = 20.0, price = 0.3),
                    dummyStation(index = 2, distance = 10.0, price = 0.3, isPublicPrice = false),
                    dummyStation(index = 3, distance = 30.0, price = 0.3),
                ),
            expected =
                listOf(
                    // "Station 2 at 10.0 km, 0.3 €", <- This one is removed
                    "Station 1 at 20.0 km, 0.3 €",
                    "Station 3 at 30.0 km, 0.3 €",
                ),
        )
    }

    @Test
    fun `do not filter non public prices if stated not to`() {
        testCase(
            onlyPublicPrices = false,
            stations =
                listOf(
                    dummyStation(index = 1, distance = 20.0, price = 0.3),
                    dummyStation(index = 2, distance = 10.0, price = 0.3, isPublicPrice = false),
                    dummyStation(index = 3, distance = 30.0, price = 0.3),
                ),
            expected =
                listOf(
                    "Station 2 at 10.0 km, 0.3 €", // <- This one not filtered
                    "Station 1 at 20.0 km, 0.3 €",
                    "Station 3 at 30.0 km, 0.3 €",
                ),
        )
    }

    @Test
    fun `non public prices do not lower the cutoff price`() {
        testCase(
            onlyPublicPrices = false,
            stations =
                listOf(
                    dummyStation(index = 1, distance = 10.0, price = 0.5),
                    dummyStation(index = 2, distance = 15.0, price = 0.3, isPublicPrice = false),
                    dummyStation(index = 3, distance = 20.0, price = 0.4),
                ),
            expected =
                listOf(
                    "Station 1 at 10.0 km, 0.5 €",
                    "Station 2 at 15.0 km, 0.3 €",
                    "Station 3 at 20.0 km, 0.4 €", // Should be filtered if 2 were public
                ),
        )
    }

    @Test
    fun `closed station`() {
        testCase(
            stations =
                listOf(
                    dummyStation(index = 1, distance = 10.0, price = 0.2),
                    dummyStation(index = 2, distance = 15.0, price = 0.3, hours = ""),
                ),
            expected =
                listOf(
                    "Station 1 at 10.0 km, 0.2 €",
                    //"Station 2 at 15.0 km, 0.3 €", // Closed
                ),
        )
    }

    @Test
    fun `closed station opening soon`() {
        atMadridInstant(DayOfWeek.TUESDAY, "09:30") { // 1:30h to 11h
            testCase(
                hideClosedMarginInMinutes = 2 * 60, // Margin 2h
                stations =
                    listOf(
                        dummyStation(index = 1, distance = 10.0, price = 0.5),
                        dummyStation(index = 2, distance = 15.0, price = 0.3, hours = "L-D: 11:00-23:00"),
                    ),
                expected =
                    listOf(
                        "Station 1 at 10.0 km, 0.5 €",
                        "Station 2 at 15.0 km, 0.3 €", // Closed but soon open!
                    ),
            )
        }
    }

    @Test
    fun `closed station opening late`() {
        atMadridInstant(DayOfWeek.TUESDAY, "08:30") { // 2:30h to 11h
            testCase(
                hideClosedMarginInMinutes = 2 * 60, // Margin 2h
                stations =
                    listOf(
                        dummyStation(index = 1, distance = 10.0, price = 0.5),
                        dummyStation(index = 2, distance = 15.0, price = 0.3, hours = "L-D: 11:00-23:00"),
                    ),
                expected =
                    listOf(
                        "Station 1 at 10.0 km, 0.5 €",
                        //"Station 2 at 15.0 km, 0.3 €", // Closed during the next 2h
                    ),
            )
        }
    }

    @Test
    fun `closed station opening late but extended margin`() {
        atMadridInstant(DayOfWeek.TUESDAY, "08:30") { // 2:30h to 11h
            testCase(
                hideClosedMarginInMinutes = 3 * 60, // Margin 3h!
                stations =
                    listOf(
                        dummyStation(index = 1, distance = 10.0, price = 0.5),
                        dummyStation(index = 2, distance = 15.0, price = 0.3, hours = "L-D: 11:00-23:00"),
                    ),
                expected =
                    listOf(
                        "Station 1 at 10.0 km, 0.5 €",
                        "Station 2 at 15.0 km, 0.3 €", // Closed but opens in less than 3h
                    ),
            )
        }
    }

    @Test
    fun `permanently closed with infinite margin (one week)`() {
        atMadridInstant(DayOfWeek.TUESDAY, "08:30") { // 2:30h to 11h
            testCase(
                hideClosedMarginInMinutes = 7 * 24 * 60, // Margin 3h!
                stations =
                    listOf(
                        dummyStation(index = 1, distance = 10.0, price = 0.5),
                        dummyStation(index = 2, distance = 15.0, price = 0.3, hours = ""),
                    ),
                expected =
                    listOf(
                        "Station 1 at 10.0 km, 0.5 €",
                        "Station 2 at 15.0 km, 0.3 €", // Infinite margin
                    ),
            )
        }
    }

    @Test
    fun `closed stations do not lower the cutoff price`() {
        atMadridInstant(DayOfWeek.TUESDAY, "10:30") { // 0:30h to 11h
            testCase(
                hideClosedMarginInMinutes = 2 * 60, // Margin 2h
                stations =
                    listOf(
                        dummyStation(index = 1, distance = 10.0, price = 0.5),
                        dummyStation(index = 2, distance = 15.0, price = 0.3, hours = "L-D: 11:00-23:00"),
                        dummyStation(index = 3, distance = 20.0, price = 0.4),
                    ),
                expected =
                    listOf(
                        "Station 1 at 10.0 km, 0.5 €",
                        "Station 2 at 15.0 km, 0.3 €",
                        "Station 3 at 20.0 km, 0.4 €", // Should be filtered if 2 were open
                    ),
            )
        }
    }
}
