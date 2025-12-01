package net.canvoki.carburoid.product

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.canvoki.carburoid.R

object ProductCatalog {
    data class Product(
        val apiName: String,
        @field:StringRes val name: Int? = null,
    )

    data class ProductCategory(
        @field:StringRes val name: Int,
        val products: List<Product>,
    )

    val categories: List<ProductCategory> by lazy {
        listOf(
            ProductCategory(
                name = R.string.product_category_diesel,
                products =
                    listOf(
                        Product("Gasoleo A", R.string.product_name_gasoleo_a),
                        Product("Gasoleo Premium"),
                        Product("Gasoleo B"),
                        Product("Diésel Renovable"),
                    ),
            ),
            ProductCategory(
                name = R.string.product_category_gasoline,
                products =
                    listOf(
                        Product("Gasolina 95 E10"),
                        Product("Gasolina 95 E25"),
                        Product("Gasolina 95 E5"),
                        Product("Gasolina 95 E5 Premium"),
                        Product("Gasolina 95 E85"),
                        Product("Gasolina 98 E10"),
                        Product("Gasolina 98 E5"),
                        Product("Gasolina Renovable"),
                    ),
            ),
            ProductCategory(
                name = R.string.product_category_natural_gas,
                products =
                    listOf(
                        Product("Gas Natural Comprimido"),
                        Product("Gas Natural Licuado"),
                        Product("Biogas Natural Comprimido"),
                        Product("Biogas Natural Licuado"),
                    ),
            ),
            ProductCategory(
                name = R.string.product_category_biofuels,
                products =
                    listOf(
                        Product("Biodiesel"),
                        Product("Bioetanol"),
                    ),
            ),
            ProductCategory(
                name = R.string.product_category_other_gases,
                products =
                    listOf(
                        Product("Gases licuados del petróleo"),
                        Product("Hidrogeno"),
                        Product("Amoniaco"),
                        Product("Metanol"),
                    ),
            ),
            ProductCategory(
                name = R.string.product_category_additives,
                products =
                    listOf(
                        Product("Adblue"),
                    ),
            ),
        )
    }

    private val productMap: Map<String, Product> by lazy {
        categories
            .flatMap { it.products }
            .associateBy { it.apiName }
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
