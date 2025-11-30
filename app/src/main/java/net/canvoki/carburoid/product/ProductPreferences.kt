package net.canvoki.carburoid.product

import android.content.Context
import androidx.core.content.edit

/// Product related data in Android Preferences
class ProductPreferences(
    private val context: Context,
) {
    companion object {
        private const val PREFS_NAME = "product_settings"
        private const val PREF_LAST_SELECTED = "last_selected_product"
        private const val DEFAULT_PRODUCT = ProductManager.DEFAULT_PRODUCT
    }

    private fun preferences() = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    fun loadLastSelectedProduct(): String =
        preferences().getString(PREF_LAST_SELECTED, DEFAULT_PRODUCT) ?: DEFAULT_PRODUCT

    fun saveLastSelectedProduct(product: String) {
        preferences().edit {
            putString(PREF_LAST_SELECTED, product)
        }
    }
}
