package net.canvoki.carburoid.distances

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.canvoki.carburoid.model.GasStation
import net.canvoki.shared.log

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
     * Stablishes if the GasStation is beyond seas.
     */
    fun isBeyondSea(station: GasStation): Boolean = method?.isBeyondSea(station) ?: false
}
