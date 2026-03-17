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
        CountryRegistry.current.landMass(
            origin.latitude,
            origin.longitude,
        )

    override fun computeDistance(station: GasStation): Float? {
        val originLoc = origin.toAndroidLocation()
        val stationLoc =
            station.latitude?.let { lat ->
                station.longitude?.let { lng ->
                    GeoPoint(
                        latitude = lat,
                        longitude = lng,
                    ).toAndroidLocation()
                }
            } ?: return null
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
            CountryRegistry.current.landMass(
                station.latitude!!,
                station.longitude!!,
            )
        val destinationLandMass =
            destination?.let {
                CountryRegistry.current.landMass(
                    it.latitude,
                    it.longitude,
                )
            }
        if (destinationLandMass == null) {
            return gasStationLandMass != originLandMass
        }

        return gasStationLandMass != originLandMass && gasStationLandMass != destinationLandMass
    }

    override suspend fun refineRoadDistances(stations: List<GasStation>) {
        if (stations.isEmpty()) return

        val originCoord = Pair(origin.latitude, origin.longitude)
        val destCoord = destination?.let { Pair(it.latitude, it.longitude) }
        val stationCoords = stations.map { Pair(it.latitude ?: 0.0, it.longitude ?: 0.0) }
        println("Refining $origin -> $destination, passing by $stations")

        val distancesFromA =
            OsrmRouting.getDistances(
                sources = listOf(originCoord),
                destinations = stationCoords,
            )[0]

        if (destCoord == null) {
            // Single-point mode: A → Sᵢ
            stations.forEachIndexed { i, station ->
                station.setRoadDistance(distancesFromA[i].toFloat())
            }
        } else {
            // Route mode: compute deviation = (A→Sᵢ + Sᵢ→B − A→B)
            val extendedSources = stationCoords + listOf(originCoord)
            val allDistancesToB =
                OsrmRouting
                    .getDistances(
                        sources = extendedSources,
                        destinations = listOf(destCoord),
                    ).map { it[0] }
            val distancesToB = allDistancesToB.dropLast(1)
            val aToB = allDistancesToB.last() // A→B
            stations.zip(distancesFromA.zip(distancesToB)) { station, (aToS, sToB) ->
                val deviation = (aToS + sToB - aToB).coerceAtLeast(0.0)
                station.setRoadDistance(deviation.toFloat())
            }
        }
    }
}
