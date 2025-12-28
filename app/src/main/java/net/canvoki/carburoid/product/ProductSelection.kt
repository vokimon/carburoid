package net.canvoki.carburoid.product

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Coordinates the global state of the current selected product.
 * That includes ProducManager and ProductPreferences.
 */
class ProductSelection(
    private val context: Context,
) {
    private var preferences = ProductPreferences(context)

    companion object {
        const val N_RECENT = 5
    }

    private fun updateRecent(product: String) {
        val recent = recent().toMutableList()
        if (product in recent) {
            recent.remove(product)
        }
        recent.add(0, product)
        preferences.saveRecentProducts(recent.take(5))
    }

    fun recent(): List<String> {
        // Either path returns at least one item:
        // the current product.
        // Other methods rely on this postcondition.

        val recent = preferences.loadRecentProducts()
        if (recent.isNotEmpty()) {
            return recent
        }

        val legacy = preferences.loadLastSelectedProduct()
        legacy?.let {
            return listOf(it)
        }

        return listOf(ProductManager.getCurrent())
    }

    fun getCurrent(): String {
        val product = recent().firstOrNull()!!
        if (product != ProductManager.getCurrent()) {
            ProductManager.setCurrent(product)
        }
        return product
    }

    fun setCurrent(product: String) {
        updateRecent(product)
        ProductManager.setCurrent(product)
    }

    fun choices(): Set<String> = ProductCatalog.availableProducts

    /**
     * Exposes the current product as Compose State.
     * Allows reading the value with `by` and updating with setter.
     */
    @Composable
    fun asState(): MutableState<String> {
        val state = remember { mutableStateOf(getCurrent()) }

        // 1. Manager ➜ UI
        LaunchedEffect(Unit) {
            ProductManager.productChanged.collect {
                state.value = getCurrent()
            }
        }

        // 2. UI ➜ Manager
        LaunchedEffect(state.value) {
            if (state.value != ProductManager.getCurrent()) {
                setCurrent(state.value)
            }
        }

        return state
    }
}
