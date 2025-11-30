package net.canvoki.carburoid.product

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatAutoCompleteTextView

// Selects the current product
class ProductSelector
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {
        private var productSelection = ProductSelection(context)

        init {
            setupListener()
            post {
                setupProducts()
            }
        }

        private fun setupProducts() {
            val products = productSelection.choices()
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
