package net.canvoki.carburoid.distances

import android.location.Location
import net.canvoki.carburoid.country.CountryRegistry
import net.canvoki.carburoid.model.GasStation

/**
 * Computes distance from a fixed reference address (e.g., city center) to each gas station.
 */
class DistanceFromAddress(
    private val referenceLocation: Location,
) : DistanceMethod {
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

        return referenceLocation.distanceTo(stationLoc)
    }

    override fun isBeyondSea(station: GasStation): Boolean {
        val gasStationLandMass =
            CountryRegistry.current.landMass(
                station.latitude!!,
                station.longitude!!,
            )
        val thisLandMass =
            CountryRegistry.current.landMass(
                referenceLocation.latitude,
                referenceLocation.longitude,
            )
        return gasStationLandMass != thisLandMass
    }
}
