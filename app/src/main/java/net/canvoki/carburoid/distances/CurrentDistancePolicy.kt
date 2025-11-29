package net.canvoki.carburoid.distances

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.canvoki.carburoid.log
import net.canvoki.carburoid.model.GasStation

/**
 * Singleton that holds the current active distance computation strategy.
 * Acts as a global policy switcher.
 */
object CurrentDistancePolicy {
    private var method: DistanceMethod? = null

    private val _methodChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val methodChanged = _methodChanged.asSharedFlow()

    /**
     * Sets the active distance computation strategy.
     */
    fun setMethod(method: DistanceMethod?) {
        this.method = method
        val result = _methodChanged.tryEmit(Unit)
    }

    /**
     * Gets the active distance computation strategy.
     */
    fun getMethod(): DistanceMethod? = this.method

    /**
     * Computes distance for the given station using the active strategy.
     */
    fun getDistance(station: GasStation): Float? = method?.computeDistance(station)

    /**
     * Returns the human-readable name of the current reference point/route.
     */
    fun getReferenceName(): String = method?.getReferenceName() ?: "Unknown Reference"
}
