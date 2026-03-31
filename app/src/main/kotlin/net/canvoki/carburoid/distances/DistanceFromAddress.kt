package net.canvoki.carburoid.distances

import net.canvoki.carburoid.country.CountryRegistry
import net.canvoki.carburoid.location.GeoPoint
import net.canvoki.carburoid.model.GasStation

/**
 * Computes distance from a fixed reference address (e.g., city center) to each gas station.
 */
class DistanceFromAddress(
    private val origin: GeoPoint,
    private val destination: GeoPoint? = null,
) : DistanceMethod {
    val originLandMass =
        CountryRegistry.current.landMass(origin)

    override fun computeDistance(station: GasStation): Float? {
        val originLoc = origin.toAndroidLocation()
        val stationLoc =
            station.geoPoint()?.let {
                it.toAndroidLocation()
            } ?: GeoPoint(latitude = 0.0, longitude = 0.0).toAndroidLocation()
        if (destination == null) {
            return originLoc.distanceTo(stationLoc)
        }
        val destinationLoc = destination.toAndroidLocation()
        return originLoc.distanceTo(stationLoc) +
            destinationLoc.distanceTo(stationLoc) -
            originLoc.distanceTo(destinationLoc)
    }

    override fun isBeyondSea(station: GasStation): Boolean {
        val gasStationLandMass =
            station.geoPoint()?.let {
                CountryRegistry.current.landMass(it)
            }
        val destinationLandMass =
            destination?.let {
                CountryRegistry.current.landMass(it)
            }
        if (destinationLandMass == null) {
            return gasStationLandMass != originLandMass
        }

        return gasStationLandMass != originLandMass && gasStationLandMass != destinationLandMass
    }

    override suspend fun refineRoadDistances(stations: List<GasStation>) {
        if (stations.isEmpty()) return
        val stationCoords = stations.map { it.geoPoint() ?: GeoPoint(latitude = 0.0, longitude = 0.0) }
        val distancesFromA =
            OsrmRouting.getDistances(
                sources = listOf(origin),
                destinations = stationCoords,
            )

        if (distancesFromA.isEmpty()) return
        if (destination == null) {
            // Single-point mode: A → Sᵢ
            stations.forEachIndexed { i, station ->
                station.setRoadDistance(distancesFromA[0][i].toFloat())
            }
        } else {
            // Route mode: compute deviation = (A→Sᵢ + Sᵢ→B − A→B)
            val extendedSources = stationCoords + listOf(origin)
            val allDistancesToB =
                OsrmRouting
                    .getDistances(
                        sources = extendedSources,
                        destinations = listOf(destination),
                    ).map { it[0] }
            if (allDistancesToB.isEmpty()) return
            val distancesToB = allDistancesToB.dropLast(1)
            val aToB = allDistancesToB.last() // A→B
            stations.zip(distancesFromA[0].zip(distancesToB)) { station, (aToS, sToB) ->
                val deviation = (aToS + sToB - aToB).coerceAtLeast(0.0)
                station.setRoadDistance(deviation.toFloat())
            }
        }
    }
}
