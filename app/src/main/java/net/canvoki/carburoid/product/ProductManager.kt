package net.canvoki.carburoid.product

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Keeps track of the current product to choose gas station price from.
 *
 * This is a state holder which can be tested independently wihout Android.
 * Change it from ProductSelection. Which also considers and updates
 * Preferences.
 */
object ProductManager {
    const val DEFAULT_PRODUCT = "Gasoleo A"

    private val _productChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val productChanged = _productChanged.asSharedFlow()

    private var current: String = DEFAULT_PRODUCT

    fun getCurrent(): String = current

    fun setCurrent(product: String) {
        current = product
        _productChanged.tryEmit(Unit)
    }

    fun resetCurrent() {
        setCurrent(DEFAULT_PRODUCT)
    }
}
