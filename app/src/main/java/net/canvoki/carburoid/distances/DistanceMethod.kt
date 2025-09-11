package net.canvoki.carburoid.distances

import android.location.Location
import net.canvoki.carburoid.model.GasStation

/**
 * Strategy interface to compute distance from a reference point/route to a gas station.
 */
interface DistanceMethod {
    /**
     * Computes distance in meters from reference to the given station.
     * Returns null if computation is not possible (e.g., missing coordinates).
     */
    fun computeDistance(station: GasStation): Float?

    /**
     * Returns a human-readable name for the reference (e.g., "Your Location").
     */
    fun getReferenceName(): String
}
