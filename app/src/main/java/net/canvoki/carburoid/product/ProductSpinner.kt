package net.canvoki.carburoid.product

import android.content.Context
import android.util.AttributeSet
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.AdapterView
import android.view.View
import net.canvoki.carburoid.model.GasStation

val products = listOf(
      "Gasoleo A",
      "Adblue",
      "Amoniaco",
      "Biodiesel",
      "Bioetanol",
      "Biogas Natural Comprimido",
      "Biogas Natural Licuado",
      "Diésel Renovable",
      "Gas Natural Comprimido",
      "Gas Natural Licuado",
      "Gases licuados del petróleo",
      "Gasoleo A",
      "Gasoleo B",
      "Gasoleo Premium",
      "Gasolina 95 E10",
      "Gasolina 95 E25",
      "Gasolina 95 E5",
      "Gasolina 95 E5 Premium",
      "Gasolina 95 E85",
      "Gasolina 98 E10",
      "Gasolina 98 E5",
      "Gasolina Renovable",
      "Hidrogeno",
      "Metanol",
)


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
        private val DEFAULT_PRODUCT = GasStation.DEFAULT_PRODUCT

    }

    init {
        setupProducts(products)
        setupListener()
    }

    private fun setupProducts(products: List<String>) {
        adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            products
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
                    GasStation.setCurrentProduct(product) // Optional
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
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_LAST_SELECTED, product).apply()
    }

    private fun loadLastSelectedProduct(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_LAST_SELECTED, DEFAULT_PRODUCT) ?: DEFAULT_PRODUCT
    }
}
