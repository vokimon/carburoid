package net.canvoki.carburoid.product

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
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

    private fun preferences() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadLastSelectedProduct(): String =
        preferences().getString(PREF_LAST_SELECTED, DEFAULT_PRODUCT) ?: DEFAULT_PRODUCT

    fun saveLastSelectedProduct(product: String) {
        preferences().edit {
            putString(PREF_LAST_SELECTED, product)
        }
    }
}

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
}

// Selects the current product
class ProductSelector
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {
        private val productSelection = ProductSelection(context)

        init {
            setupListener()
            post {
                setupProducts()
            }
        }

        private fun setupProducts() {
            val products = ProductManager.available()
            setAdapter(
                ArrayAdapter(
                    context,
                    android.R.layout.simple_dropdown_item_1line,
                    products,
                ),
            )
            val selected = productSelection.getCurrent()
            setText(selected, false)
        }

        private fun setupListener() {
            setOnItemClickListener { parent, _, position, _ ->
                val product = parent.getItemAtPosition(position) as String
                productSelection.setCurrent(product)
                setText(productSelection.getCurrent(), false)
            }
        }

        override fun onRestoreInstanceState(state: Parcelable?) {
            super.onRestoreInstanceState(state)
            post {
                setupProducts()
            }
        }
    }
