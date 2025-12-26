package net.canvoki.carburoid.product

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

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
