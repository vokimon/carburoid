package net.canvoki.carburoid.distances

import kotlin.math.abs

data class BarcelonaGridPoint(
    val latitude: Double,
    val longitude: Double,
    val vIndex: Int, // Vertical axis (NW–SE streets: Urgell, Casanova, Balmes...)
    val hIndex: Int, // Horizontal axis (NE–SW avenues: Paris, Valencia, Aragó...)
) {
    val coordinate: Pair<Double, Double> get() = latitude to longitude

    fun manhattanBlocksTo(other: BarcelonaGridPoint): Int = abs(vIndex - other.vIndex) + abs(hIndex - other.hIndex)
}

object BarcelonaGrid {
    // Horizontal axis (H): NE–SW avenues (Paris → Aragó)
    private const val PARIS = 0 // Besos
    private const val CORSEGA = 1 // Llobregat
    private const val ROSSELLÓ = 2 // Besos
    private const val PROVENÇA = 3 // Llobregat
    private const val MALLORCA = 4 // Llobregat
    private const val VALENCIA = 5 // Besos
    private const val ARAGÓ = 6 // Llobregat

    // Vertical axis (V): NW–SE streets (Urgell → Balmes)
    private const val URGELL = 0 // Muntanya
    private const val VILARROEL = 1 // Mar
    private const val CASANOVA = 2 // Muntanya
    private const val MUNTANER = 3 // Mar
    private const val ARIBAU = 4 // Muntanya
    private const val GRANADOS = 5 // Mar
    private const val BALMES = 6 // Mar

    // Points used in OsrmRoutingTest
    val CASANOVA_PARIS =
        BarcelonaGridPoint(
            latitude = 41.391692,
            longitude = 2.150713,
            vIndex = CASANOVA,
            hIndex = PARIS,
        )

    val BALMES_VALENCIA =
        BarcelonaGridPoint(
            latitude = 41.390767,
            longitude = 2.160683,
            vIndex = BALMES,
            hIndex = VALENCIA,
        )

    val URGELL_PARIS =
        BarcelonaGridPoint(
            latitude = 41.389879,
            longitude = 2.148299,
            vIndex = URGELL,
            hIndex = PARIS,
        )

    val MUNTANER_VALENCIA =
        BarcelonaGridPoint(
            latitude = 41.388200,
            longitude = 2.157320,
            vIndex = MUNTANER,
            hIndex = VALENCIA,
        )

    val MUNTANER_ARAGO =
        BarcelonaGridPoint(
            vIndex = MUNTANER,
            hIndex = ARAGÓ,
            latitude = 41.387322,
            longitude = 2.158481,
        )

    val BALMES_MALLORCA =
        BarcelonaGridPoint(
            latitude = 41.391644,
            longitude = 2.159683,
            vIndex = BALMES,
            hIndex = MALLORCA,
        )
}
