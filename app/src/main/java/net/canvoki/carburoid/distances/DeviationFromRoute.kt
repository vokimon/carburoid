package net.canvoki.carburoid.distances

import android.location.Location
import net.canvoki.carburoid.model.GasStation

/**
 * Computes minimum deviation (in meters) from a predefined route (polyline) to each gas station.
 * TODO: Implement real logic to compute distance to closest segment of the polyline.
 */
class DeviationFromRoute(private val routePolyline: List<Location>) : DistanceMethod {

    override fun computeDistance(station: GasStation): Float? {
        // TODO: Compute minimum distance from station to any segment of the route
        // For now, return 0f as placeholder
        return 0f
    }

    override fun getReferenceName(): String = "Route Deviation"
}
