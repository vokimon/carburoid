package net.canvoki.carburoid.distances

import android.location.Location
import net.canvoki.carburoid.model.GasStation

/**
 * Computes distance from user's current GPS location to each gas station.
 */
class DistanceFromCurrentPosition(private val userLocation: Location?) : DistanceMethod {

    override fun computeDistance(station: GasStation): Float? {
        val stationLoc = station.latitude?.let { lat ->
            station.longitude?.let { lng ->
                Location("").apply {
                    latitude = lat
                    longitude = lng
                }
            }
        } ?: return null

        return userLocation?.distanceTo(stationLoc)
    }

    override fun getReferenceName(): String = "Your Location"
}
