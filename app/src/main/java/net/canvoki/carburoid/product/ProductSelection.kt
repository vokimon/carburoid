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
