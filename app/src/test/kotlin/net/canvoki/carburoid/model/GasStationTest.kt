package net.canvoki.carburoid.model

import net.canvoki.carburoid.distances.CurrentDistancePolicy
import net.canvoki.carburoid.distances.DistanceMethod
import net.canvoki.carburoid.test.madridInstant
import net.canvoki.shared.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import kotlin.test.fail

class GasStationTest {
    private fun createTestStation(
        latitude: Double = 41.3925,
        longitude: Double = 2.1565,
    ): GasStation =
        object : BaseGasStation() {
            override val id: Int = 1
            override val name: String? = "Test Station"
            override val address: String? = null
            override val city: String? = null
            override val state: String? = null
            override val latitude: Double? = latitude
            override val longitude: Double? = longitude
            override val isPublicPrice: Boolean = true
            override val openingHours: OpeningHours? = null
            override val prices: Map<String, Double?> = mapOf("gasoline95" to 1.5)

            override fun toJson(): String = "{}"
        }

    private var originalPolicy: DistanceMethod? = null

    @Before
    fun setUp() {
        originalPolicy = CurrentDistancePolicy.getMethod()
        CurrentDistancePolicy.setMethod(
            object : DistanceMethod {
                override fun computeDistance(station: GasStation): Float? = 1000f

                override fun isBeyondSea(station: GasStation): Boolean = false
            },
        )
    }

    @After
    fun tearDown() {
        CurrentDistancePolicy.setMethod(originalPolicy)
    }

    @Test
    fun `road distance not available by default`() {
        val station = createTestStation()
        assertEquals(false, station.hasRoadDistance())
    }

    @Test
    fun `road distance not available after compute crow distance`() {
        val station = createTestStation()
        station.computeDistance()
        assertEquals(false, station.hasRoadDistance())
    }

    @Test
    fun `road distance available after seting it`() {
        val station = createTestStation()
        station.computeDistance()
        val crowDistance = station.distanceInMeters!!

        station.setRoadDistance(crowDistance + 10.0f)

        assertEquals(true, station.hasRoadDistance())
        assertEquals(crowDistance + 10.0f, station.distanceInMeters)
    }

    @Test
    fun `road distance reset after recompute`() {
        val station = createTestStation()
        station.computeDistance()
        val crowDistance = station.distanceInMeters!!
        station.setRoadDistance(crowDistance + 10.0f)

        station.computeDistance()

        assertEquals(false, station.hasRoadDistance())
        assertEquals(crowDistance, station.distanceInMeters)
    }
}
