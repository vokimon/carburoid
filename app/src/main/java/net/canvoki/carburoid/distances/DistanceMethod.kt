package net.canvoki.carburoid.distances

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
     * Tells whether the coordinates are considered in a different land mass.
     */
    fun isBeyondSea(station: GasStation): Boolean
}
