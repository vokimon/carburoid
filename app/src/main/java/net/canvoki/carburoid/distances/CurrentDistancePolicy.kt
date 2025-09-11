package net.canvoki.carburoid.distances

import net.canvoki.carburoid.model.GasStation

/**
 * Singleton that holds the current active distance computation strategy.
 * Acts as a global policy switcher.
 */
object CurrentDistancePolicy {
    private var method: DistanceMethod? = null

    /**
     * Sets the active distance computation strategy.
     */
    fun setMethod(method: DistanceMethod) {
        this.method = method
    }

    /**
     * Computes distance for the given station using the active strategy.
     */
    fun getDistance(station: GasStation): Float? {
        return method?.computeDistance(station)
    }

    /**
     * Returns the human-readable name of the current reference point/route.
     */
    fun getReferenceName(): String {
        return method?.getReferenceName() ?: "Unknown Reference"
    }
}
