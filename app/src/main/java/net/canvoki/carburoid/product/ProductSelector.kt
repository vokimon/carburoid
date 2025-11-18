package net.canvoki.carburoid.product
import android.content.Context
import android.content.SharedPreferences
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.core.content.edit

class ProductSelector
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {
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
            setAdapter(
                ArrayAdapter(
                    context,
                    android.R.layout.simple_dropdown_item_1line,
                    products,
                ),
            )

            val selected = loadLastSelectedProduct()
            setText(selected, false)
        }

        private fun setupListener() {
            setOnItemClickListener { parent, view, position, id ->
                val product = parent.getItemAtPosition(position) as String
                if (!suppressCallback) {
                    saveLastSelectedProduct(product)
                    ProductManager.setCurrent(product)
                    listener?.invoke(product)
                }
            }

            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val currentText = text.toString()
                    val products = ProductManager.available()
                    if (currentText !in products) {
                        setText(loadLastSelectedProduct(), false)
                    }
                }
            }
        }

        fun setOnProductSelectedListener(callback: (String) -> Unit) {
            this.listener = callback
        }

        val selectedProduct: String
            get() = text.toString()

        fun setSelectedProduct(product: String) {
            suppressCallback = true
            setText(product, false)
            suppressCallback = false
        }

        private fun saveLastSelectedProduct(product: String) {
            preferences().edit {
                putString(PREF_LAST_SELECTED, product)
            }
        }

        private fun loadLastSelectedProduct(): String {
            return preferences().getString(PREF_LAST_SELECTED, DEFAULT_PRODUCT) ?: DEFAULT_PRODUCT
        }

        private fun preferences(): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        override fun onRestoreInstanceState(state: Parcelable?) {
            super.onRestoreInstanceState(state)
            post {
                setupProducts()
            }
        }
    }
