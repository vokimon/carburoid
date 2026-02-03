package net.canvoki.carburoid.product

import net.canvoki.carburoid.R

object FranceProductCatalog : ProductCatalogProvider() {
    override val categories: List<ProductCategory> by lazy {
        listOf(
            ProductCategory(
                name = R.string.product_category_diesel,
                products =
                    listOf(
                        Product("gazole", R.string.product_name_gasoleo_a),
                    ),
            ),
            ProductCategory(
                name = R.string.product_category_gasoline,
                products =
                    listOf(
                        Product("sp95", R.string.product_name_gasolina_95_e5),
                        Product("sp98", R.string.product_name_gasolina_98_e5),
                        Product("e10", R.string.product_name_gasolina_95_e10),
                        Product("e85", R.string.product_name_gasolina_95_e85),
                    ),
            ),
            ProductCategory(
                name = R.string.product_category_gaseous,
                products =
                    listOf(
                        Product("gplc", R.string.product_name_gases_licuados_del_petroleo),
                    ),
            ),
        )
    }
}
