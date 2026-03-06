package net.canvoki.carburoid.distance

import kotlinx.coroutines.runBlocking
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

    fun barcelonaEixampleDistance(blocks: Int): ClosedRange<Double> {
        val expectedDistance = blocks.toDouble() * BARCELONA_EIXAMPLE_BLOCK_DISTANCE
        val min = expectedDistance * (1 - BARCELONA_EIXAMPLE_ERROR_PERCENT)
        val max = expectedDistance * (1 + BARCELONA_EIXAMPLE_ERROR_PERCENT)
        return min..max
    }

    fun assertBarcelonaDistance(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double,
        expectedBlocks: Int,
    ) {
        skipOn<java.net.SocketTimeoutException> {
            val roadDistance =
                runBlocking {
                    OsrmRouting.getDistances(fromLat to fromLon, listOf(toLat to toLon))[0]
                }
            val expectedBounds = barcelonaEixampleDistance(expectedBlocks)
            assertEquals(
                roadDistance,
                expectedBlocks * BARCELONA_EIXAMPLE_BLOCK_DISTANCE,
                delta = expectedBlocks * BARCELONA_EIXAMPLE_BLOCK_DISTANCE * BARCELONA_EIXAMPLE_ERROR_PERCENT,
                "Expected $expectedBounds m for $expectedBlocks-block route, got $roadDistance m",
            )
        }
    }

    @Test
    fun `route Paris-Casanova to Valencia-Balmes should match 15-block grid distance`() {
        assertBarcelonaDistance(
            // Paris con casanova
            fromLat = 41.391692,
            fromLon = 2.150713,
            // Balmes x Valencia
            toLat = 41.390767,
            toLon = 2.160683,
            // 4 Besós (Casanova -> Muntaner -> Aribau -> Granados -> Balmes)
            // 5 Mar (Paris -> Corsega -> Rosselló -> Provença -> Mallorca -> València)
            expectedBlocks = 4 + 5,
        )
    }

    @Test
    fun `route Urgell-Paris to Valencia-Muntaner should match 15-block grid distance`() {
        assertBarcelonaDistance(
            // From Urgell x Paris
            fromLat = 41.389879,
            fromLon = 2.148299,
            // To Valencia x Muntaner
            toLat = 41.388200,
            toLon = 2.157320,
            // 3 Besós (Urgel -> Vilarroel -> Casanova -> Muntaner)
            // 5 Mar (Paris -> Corsega -> Rosselló -> Provença -> Mallorca -> València)
            expectedBlocks = 3 + 5,
        )
    }

    @Test
    fun `route Arago-Muntaner to Mallorca-Balmes should match 10-block grid distance`() {
        assertBarcelonaDistance(
            // From: Carrer d’Aragó ∩ Carrer de Muntaner
            fromLat = 41.3925,
            fromLon = 2.1520,
            // To: Carrer de Mallorca ∩ Carrer de Balmes
            toLat = 41.3970,
            toLon = 2.1580,
            // ~4 blocks N-S + ~6 blocks E-W
            expectedBlocks = 4 + 6,
        )
    }
}
