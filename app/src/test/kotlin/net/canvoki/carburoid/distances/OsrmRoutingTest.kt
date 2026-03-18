package net.canvoki.carburoid.distances

import kotlinx.coroutines.runBlocking
import net.canvoki.carburoid.location.GeoPoint
import net.canvoki.shared.log
import net.canvoki.shared.test.assertEquals
import net.canvoki.shared.test.skipOn
import kotlin.test.Test
import kotlin.test.assertTrue

class OsrmRoutingTest {
    // Constants based on Cerdà's Eixample grid
    private companion object {
        const val BARCELONA_EIXAMPLE_BLOCK_DISTANCE = 130.0 // meters per block (street-to-street)
        const val BARCELONA_EIXAMPLE_ERROR_PERCENT = 0.15 // ±15% tolerance for one-way detours, etc.
    }

    fun assertBarcelonaDistance(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double,
        expectedBlocks: Int?,
    ) {
        skipOn<java.net.SocketTimeoutException> {
            val roadDistances =
                runBlocking {
                    OsrmRouting.getDistances(
                        listOf(GeoPoint(latitude = fromLat, longitude = fromLon)),
                        listOf(GeoPoint(latitude = toLat, longitude = toLon)),
                    )
                }
            if (expectedBlocks == null) {
                assertEquals(roadDistances, emptyList<List<Double>>())
                return
            }
            val roadDistance = roadDistances[0][0]
            val expectedDistance = expectedBlocks * BARCELONA_EIXAMPLE_BLOCK_DISTANCE
            assertEquals(
                expectedDistance,
                roadDistance,
                delta = expectedDistance * BARCELONA_EIXAMPLE_ERROR_PERCENT,
                "Expected $expectedDistance m for $expectedBlocks-block route, " +
                    "got $roadDistance m (~${roadDistance / BARCELONA_EIXAMPLE_BLOCK_DISTANCE} blocks)",
            )
        }
    }

    fun assertBarcelonaDistance(
        from: BarcelonaGridPoint,
        to: BarcelonaGridPoint,
    ) {
        assertBarcelonaDistance(
            fromLat = from.latitude,
            fromLon = from.longitude,
            toLat = to.latitude,
            toLon = to.longitude,
            expectedBlocks = from.manhattanBlocksTo(to),
        )
    }

    @Test
    fun `route Casanova-Paris to Balmes-Valencia should match 9-block grid distance`() {
        assertBarcelonaDistance(
            BarcelonaGrid.CASANOVA_PARIS,
            BarcelonaGrid.BALMES_VALENCIA,
        )
    }

    @Test
    fun `route Urgel-Paris to Muntaner-Valencia should match 8-block grid distance`() {
        assertBarcelonaDistance(
            BarcelonaGrid.URGELL_PARIS,
            BarcelonaGrid.MUNTANER_VALENCIA,
        )
    }

    @Test
    fun `route Balmes-Mallorca to Muntaner-Arago should match 5-block grid distance`() {
        assertBarcelonaDistance(
            BarcelonaGrid.BALMES_MALLORCA,
            BarcelonaGrid.MUNTANER_ARAGO,
        )
    }

    @Test
    fun `route with errors`() {
        assertBarcelonaDistance(
            fromLat = 0.0,
            fromLon = 0.0,
            toLat = BarcelonaGrid.MUNTANER_ARAGO.latitude,
            toLon = BarcelonaGrid.MUNTANER_ARAGO.longitude,
            expectedBlocks = null, // Error
        )
    }
}
