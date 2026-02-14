package net.canvoki.carburoid.product

import net.canvoki.carburoid.R

object FranceProductCatalog : ProductCatalogProvider() {
    override val categories: List<ProductCategory> by lazy {
        listOf(
            ProductCategory(
                name = R.string.product_category_diesel,
                products =
                    listOf(
                        Product("Gasoleo A", R.string.product_name_gasoleo_a),
                    ),
            ),
            ProductCategory(
                name = R.string.product_category_gasoline,
                products =
                    listOf(
                        Product("Gasolina 95 E5", R.string.product_name_gasolina_95_e5),
                        Product("Gasolina 98 E5", R.string.product_name_gasolina_98_e5),
                        Product("Gasolina 95 E10", R.string.product_name_gasolina_95_e10),
                        Product("Gasolina 95 E85", R.string.product_name_gasolina_95_e85),
                    ),
            ),
            ProductCategory(
                name = R.string.product_category_gaseous,
                products =
                    listOf(
                        Product("Gases licuados del petróleo", R.string.product_name_gases_licuados_del_petroleo),
                    ),
            ),
        )
    }
}
