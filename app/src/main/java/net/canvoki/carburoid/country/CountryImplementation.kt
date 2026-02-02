package net.canvoki.carburoid.country

import net.canvoki.carburoid.model.GasStationResponse
import net.canvoki.carburoid.network.GasStationApi
import net.canvoki.carburoid.product.ProductCatalogProvider

interface CountryImplementation {
    val countryCode: String
    val api: GasStationApi
    val productCatalog: ProductCatalogProvider

    fun parse(json: String): GasStationResponse

    // TODO: product catalog, land masses...
}
