package net.canvoki.carburoid.distances

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import net.canvoki.carburoid.location.GeoPoint
import net.canvoki.carburoid.model.BaseGasStation
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.model.OpeningHours
import net.canvoki.shared.test.assertEquals
import net.canvoki.shared.test.skipOn
import net.canvoki.shared.test.yieldUntilIdle
import org.junit.Test
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class CurrentDistancePolicyTest {
    @Test
    fun `test distance policy method change triggers notification`() =
        runTest {
            val geoPoint =
                GeoPoint(
                    latitude = 40.7128,
                    longitude = -74.0060,
                )
            val collector = mutableListOf<Unit>()

            val collectorJob =
                launch {
                    CurrentDistancePolicy.methodChanged.collect {
                        collector.add(Unit)
                    }
                }

            yieldUntilIdle()

            assertTrue(collector.isEmpty(), "Collector should have no events.")

            val newDistanceMethod = DistanceFromAddress(geoPoint)
            CurrentDistancePolicy.setMethod(newDistanceMethod)
            yieldUntilIdle()

            assertTrue(collector.isNotEmpty(), "Collector was not notified when the method changed.")

            collectorJob.cancel()
        }

    @Test
    fun `refineRoadDistances`() =
        runBlocking {
            skipOn<java.net.SocketTimeoutException> {
                val origin = locationOf(BarcelonaGrid.CASANOVA_PARIS)
                val destination = locationOf(BarcelonaGrid.BALMES_VALENCIA)
                println("Within test: $origin -> $destination")
                val method = DistanceFromAddress(origin, destination)

                val stations =
                    listOf(
                        createStation(1, BarcelonaGrid.URGELL_PARIS),
                        createStation(2, BarcelonaGrid.MUNTANER_VALENCIA),
                        createStation(3, BarcelonaGrid.MUNTANER_ARAGO),
                    )

                // Ensure they start with crow
                stations.forEach { it.computeDistance() }

                // Act
                method.refineRoadDistances(stations)

                // Assert: distance value + source (road/crow)
                val actual =
                    stations.joinToString("\n") { station ->
                        val source = if (station.hasRoadDistance()) "road" else "crow"
                        val distance = station.distanceInMeters?.let { "%.0f".format(it) } ?: "null"
                        "$distance $source"
                    }

                val expected =
                    """
                    1537 road
                    803 road
                    1313 road
                    """.trimIndent()

                assertEquals(expected, actual)
            }
        }

    private fun createStation(
        id: Int,
        point: BarcelonaGridPoint,
    ): GasStation =
        object : BaseGasStation() {
            override val id = id
            override val name = "$id"
            override val latitude = point.latitude
            override val longitude = point.longitude
            override val price = 1.5
            override val isPublicPrice = true
            override val address = ""
            override val city = "Barcelona"
            override val state = "Barcelona"
            override val openingHours = OpeningHours.parse("")
            override val prices = emptyMap<String, Double?>()

            override fun toJson() = ""
        }

    private fun locationOf(point: BarcelonaGridPoint) =
        GeoPoint(
            latitude = point.latitude,
            longitude = point.longitude,
        )
}
