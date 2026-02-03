package net.canvoki.carburoid.product

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.canvoki.carburoid.R
import net.canvoki.carburoid.country.CountryRegistry

abstract class ProductCatalogProvider {
    abstract val categories: List<ProductCategory>
    val availableProducts: Set<String> by lazy {
        categories.flatMap { it.products }.map { it.apiName }.toSet()
    }
    val productMap: Map<String, Product> by lazy {
        categories.flatMap { it.products }.associateBy { it.apiName }
    }
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

    private var provider: ProductCatalogProvider = CountryRegistry.current.productCatalog

    val categories: List<ProductCategory> get() = provider.categories

    val availableProducts: Set<String> get() = provider.availableProducts

    fun productName(
        apiName: String,
        context: Context,
    ): String {
        val product = provider.productMap[apiName]
        return product?.name?.let { context.getString(it) } ?: apiName
    }

    @Composable
    fun productName(apiName: String): String {
        val product = provider.productMap[apiName]
        return product?.name?.let { stringResource(it) } ?: apiName
    }
}

@Composable
fun translateProductName(apiName: String): String = ProductCatalog.productName(apiName)

fun translateProductName(
    apiName: String,
    context: Context,
): String = ProductCatalog.productName(apiName, context)
