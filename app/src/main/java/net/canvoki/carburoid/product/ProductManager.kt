package net.canvoki.carburoid.product

import net.canvoki.carburoid.model.GasStation

object ProductManager {
    private val products = listOf(
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

    private var current: String = DEFAULT_PRODUCT

    const val DEFAULT_PRODUCT = "Gasoleo A"

    fun available(): List<String> = products

    fun getCurrent(): String = current

    fun setCurrent(product: String) {
        current = product
    }

    fun resetCurrent() {
        setCurrent(DEFAULT_PRODUCT)
    }
}
