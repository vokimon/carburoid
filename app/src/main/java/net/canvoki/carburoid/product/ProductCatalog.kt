package net.canvoki.carburoid.product

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.canvoki.carburoid.R

interface ProductCatalogProvider {
    val categories: List<ProductCategory>
    val availableProducts: Set<String>
        get() = categories.flatMap { it.products }.map { it.apiName }.toSet()
    val productMap: Map<String, Product>
        get() = categories.flatMap { it.products }.associateBy { it.apiName }
}

data class Product(
    val apiName: String,
    @field:StringRes val name: Int? = null,
)

data class ProductCategory(
    @field:StringRes val name: Int,
    val products: List<Product>,
)

object ProductCatalog {
    typealias Product = net.canvoki.carburoid.product.Product
    typealias ProductCategory = net.canvoki.carburoid.product.ProductCategory

    val categories: List<ProductCategory> by lazy {
        listOf(
            ProductCategory(
                name = R.string.product_category_additives,
                products =
                    listOf(
                        Product("Adblue", R.string.product_name_adblue),
                    ),
            ),
            ProductCategory(
                name = R.string.product_category_diesel,
                products =
                    listOf(
                        Product("Gasoleo A", R.string.product_name_gasoleo_a),
                        Product("Gasoleo Premium", R.string.product_name_gasoleo_premium),
                        Product("Gasoleo B", R.string.product_name_gasoleo_b),
                        Product("Diésel Renovable", R.string.product_name_diesel_renovable),
                        Product("Biodiesel", R.string.product_name_biodiesel),
                    ),
            ),
            ProductCategory(
                name = R.string.product_category_gasoline,
                products =
                    listOf(
                        Product("Gasolina 95 E5", R.string.product_name_gasolina_95_e5),
                        Product("Gasolina 98 E5", R.string.product_name_gasolina_98_e5),
                        Product("Gasolina 95 E5 Premium", R.string.product_name_gasolina_95_e5_premium),
                        Product("Gasolina 95 E10", R.string.product_name_gasolina_95_e10),
                        Product("Gasolina 98 E10", R.string.product_name_gasolina_98_e10),
                        Product("Gasolina 95 E25", R.string.product_name_gasolina_95_e25),
                        Product("Gasolina 95 E85", R.string.product_name_gasolina_95_e85),
                        Product("Gasolina Renovable", R.string.product_name_gasolina_renovable),
                        Product("Bioetanol", R.string.product_name_bioetanol),
                    ),
            ),
            ProductCategory(
                name = R.string.product_category_gaseous,
                products =
                    listOf(
                        Product("Gases licuados del petróleo", R.string.product_name_gases_licuados_del_petroleo),
                        Product("Gas Natural Comprimido", R.string.product_name_gas_natural_comprimido),
                        Product("Gas Natural Licuado", R.string.product_name_gas_natural_licuado),
                        Product("Biogas Natural Comprimido", R.string.product_name_biogas_natural_comprimido),
                        Product("Biogas Natural Licuado", R.string.product_name_biogas_natural_licuado),
                        Product("Hidrogeno", R.string.product_name_hidrogeno),
                        Product("Amoniaco", R.string.product_name_amoniaco),
                        Product("Metanol", R.string.product_name_metanol),
                    ),
            ),
        )
    }

    val availableProducts: Set<String> by lazy {
        categories
            .asSequence()
            .flatMap { it.products.asSequence() }
            .map { it.apiName }
            .toSet()
    }

    private val productMap: Map<String, Product> by lazy {
        categories.flatMap { it.products }.associateBy { it.apiName }
    }

    fun productName(
        apiName: String,
        context: Context,
    ): String {
        val product = productMap[apiName]
        return product?.name?.let { context.getString(it) } ?: apiName
    }

    @Composable
    fun productName(apiName: String): String {
        val product = productMap[apiName]
        return product?.name?.let { stringResource(it) } ?: apiName
    }
}

@Composable
fun translateProductName(apiName: String): String = ProductCatalog.productName(apiName)

fun translateProductName(
    apiName: String,
    context: Context,
): String = ProductCatalog.productName(apiName, context)
