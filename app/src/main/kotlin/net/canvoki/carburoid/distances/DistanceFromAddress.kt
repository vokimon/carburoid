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
}
