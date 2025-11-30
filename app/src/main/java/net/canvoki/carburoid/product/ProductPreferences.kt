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
        private const val PREF_RECENT_PRODUCTS = "recent_products"
    }

    private fun preferences() = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    fun loadLastSelectedProduct(): String? =
        preferences().getString(PREF_LAST_SELECTED, null)

    fun saveLastSelectedProduct(product: String) {
        preferences().edit {
            putString(PREF_LAST_SELECTED, product)
        }
    }
    fun loadRecentProducts(): List<String> {
        val recentProducts = preferences().getString(PREF_RECENT_PRODUCTS, null)
        if (recentProducts.isNullOrBlank()) {
            return emptyList()
        }
        return recentProducts.split(",")
    }

    fun saveRecentProducts(products: List<String>) {
        preferences().edit {
            putString(PREF_RECENT_PRODUCTS, products.joinToString(","))
        }
    }
}
