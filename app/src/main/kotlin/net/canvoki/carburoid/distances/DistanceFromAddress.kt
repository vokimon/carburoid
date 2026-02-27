package net.canvoki.carburoid.distances

import android.location.Location
import net.canvoki.carburoid.country.CountryRegistry
import net.canvoki.carburoid.model.GasStation

/**
 * Computes distance from a fixed reference address (e.g., city center) to each gas station.
 */
class DistanceFromAddress(
    private val origin: Location,
    private val destination: Location? = null,
) : DistanceMethod {
    val originLandMass =
        CountryRegistry.current.landMass(
            origin.latitude,
            origin.longitude,
        )

    override fun computeDistance(station: GasStation): Float? {
        val stationLoc =
            station.latitude?.let { lat ->
                station.longitude?.let { lng ->
                    Location("").apply {
                        latitude = lat
                        longitude = lng
                    }
                }
            } ?: return null
        if (destination == null) {
            return origin.distanceTo(stationLoc)
        }
        return origin.distanceTo(stationLoc) + destination.distanceTo(stationLoc) - origin.distanceTo(destination)
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
