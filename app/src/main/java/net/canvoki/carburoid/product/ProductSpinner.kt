package net.canvoki.carburoid.product

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.AdapterView
import android.view.View
import net.canvoki.carburoid.product.ProductManager


class ProductSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.spinnerStyle
) : Spinner(context, attrs, defStyleAttr) {

    private var listener: ((String) -> Unit)? = null
    private var suppressCallback = false

    companion object {
        private const val PREFS_NAME = "product_settings"
        private const val PREF_LAST_SELECTED = "last_selected_product"
        private val DEFAULT_PRODUCT = ProductManager.DEFAULT_PRODUCT
    }

    init {
        setupProducts()
        setupListener()
    }

    private fun setupProducts() {
        val products = ProductManager.available()
        adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            products,
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val selected = loadLastSelectedProduct()
        val index = products.indexOf(selected).takeIf { it >= 0 } ?: 0

        suppressCallback = true
        setSelection(index)
        suppressCallback = false
    }

    private fun setupListener() {
        onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val product = adapter.getItem(position) as String? ?: return
                if (!suppressCallback) {
                    saveLastSelectedProduct(product)
                    ProductManager.setCurrent(product) // Optional
                    listener?.invoke(product)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // no-op
            }
        }
    }

    fun setOnProductSelectedListener(callback: (String) -> Unit) {
        this.listener = callback
    }

    val selectedProduct: String?
        get() = selectedItem as? String

    private fun saveLastSelectedProduct(product: String) {
        preferences().edit().putString(PREF_LAST_SELECTED, product).apply()
    }

    private fun loadLastSelectedProduct(): String {
        return preferences().getString(PREF_LAST_SELECTED, DEFAULT_PRODUCT) ?: DEFAULT_PRODUCT
    }

    private fun preferences() : SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
