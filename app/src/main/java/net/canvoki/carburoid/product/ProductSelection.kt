package net.canvoki.carburoid.product

import android.content.Context

// Coordinates the global state of the current selected product
class ProductSelection(
    private val context: Context,
) {
    private var preferences = ProductPreferences(context)

    fun getCurrent(): String {
        val product = preferences.loadLastSelectedProduct()
        if (product != ProductManager.getCurrent()) {
            ProductManager.setCurrent(product)
        }
        return product
    }

    fun setCurrent(product: String) {
        preferences.saveLastSelectedProduct(product)
        ProductManager.setCurrent(product)
    }

    fun choices(): List<String> = ProductManager.available()
}
