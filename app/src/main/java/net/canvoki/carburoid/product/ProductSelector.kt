package net.canvoki.carburoid.product

import android.content.Context
import android.content.SharedPreferences
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.core.content.edit

class ProductPreferences(
    private val context: Context,
) {
    companion object {
        private const val PREFS_NAME = "product_settings"
        private const val PREF_LAST_SELECTED = "last_selected_product"
        private const val DEFAULT_PRODUCT = ProductManager.DEFAULT_PRODUCT
    }

    private fun preferences(): SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadLastSelectedProduct(): String =
        preferences().getString(PREF_LAST_SELECTED, DEFAULT_PRODUCT) ?: DEFAULT_PRODUCT

    private fun saveLastSelectedProduct(product: String) {
        preferences().edit {
            putString(PREF_LAST_SELECTED, product)
        }
    }

    fun getCurrent(): String {
        val product = loadLastSelectedProduct()
        if (product != ProductManager.getCurrent()) {
            ProductManager.setCurrent(product)
        }
        return product
    }

    fun setCurrent(product: String) {
        saveLastSelectedProduct(product)
        ProductManager.setCurrent(product)
    }
}

class ProductSelector
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {
        private var preferences = ProductPreferences(context)

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
            val selected = preferences.getCurrent()
            setText(selected, false)
        }

        private fun setupListener() {
            setOnItemClickListener { parent, view, position, id ->
                val product = parent.getItemAtPosition(position) as String
                preferences.setCurrent(product)
                setText(preferences.getCurrent(), false)
            }
        }

        override fun onRestoreInstanceState(state: Parcelable?) {
            super.onRestoreInstanceState(state)
            post {
                setupProducts()
            }
        }
    }
