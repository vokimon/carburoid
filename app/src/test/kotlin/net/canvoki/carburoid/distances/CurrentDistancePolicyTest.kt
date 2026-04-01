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
    fun `refineRoadDistances with destination`() =
        runBlocking {
            assertRefinedDistances(
                origin = BarcelonaGrid.CASANOVA_PARIS,
                destination = BarcelonaGrid.BALMES_VALENCIA,
                expected =
                    listOf(
                        "1537 road",
                        "803 road",
                        "1313 road",
                    ),
                BarcelonaGrid.URGELL_PARIS,
                BarcelonaGrid.MUNTANER_VALENCIA,
                BarcelonaGrid.MUNTANER_ARAGO,
            )
        }

    @Test
    fun `refineRoadDistances with NO destination`() =
        runBlocking {
            assertRefinedDistances(
                origin = BarcelonaGrid.CASANOVA_PARIS,
                destination = null,
                expected =
                    listOf(
                        "1252 road",
                        "784 road",
                        "922 road",
                    ),
                BarcelonaGrid.URGELL_PARIS,
                BarcelonaGrid.MUNTANER_VALENCIA,
                BarcelonaGrid.MUNTANER_ARAGO,
            )
        }

    private suspend fun assertRefinedDistances(
        origin: BarcelonaGridPoint,
        destination: BarcelonaGridPoint?,
        expected: List<String>,
        vararg stations: BarcelonaGridPoint,
    ) {
        skipOn<java.net.SocketTimeoutException> {
            val method =
                DistanceFromAddress(
                    origin = origin.toGeoPoint(),
                    destination = destination?.toGeoPoint(),
                )

            val stationObjects =
                stations.mapIndexed { index, point ->
                    object : BaseGasStation() {
                        override val id = index
                        override val name = "S$index"
                        override val latitude = point.latitude
                        override val longitude = point.longitude
                        override val price = 1.5
                        override val isPublicPrice = true
                        override val address = ""
                        override val city = "Barcelona"
                        override val state = "Barcelona"
                        override val openingHours = OpeningHours.parse("")
                        override val prices = emptyMap<String, Double?>()

                        val geoPoint = point.toGeoPoint()

                        override fun toJson() = ""

                        override fun toString() = "${geoPoint.pretty()}"
                    }
                }

            stationObjects.forEach { it.computeDistance() }
            method.refineRoadDistances(stationObjects)

            val actual =
                stationObjects.joinToString("\n") { station ->
                    val source = if (station.hasRoadDistance()) "road" else "crow"
                    val distance = station.distanceInMeters?.let { "%.0f".format(it) } ?: "null"
                    "$distance $source"
                }

            val expectedString = expected.joinToString("\n")
            assertEquals(expectedString, actual)
        }
    }
}
