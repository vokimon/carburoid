package net.canvoki.carburoid.country

import androidx.annotation.StringRes
import net.canvoki.carburoid.distances.LandMass
import net.canvoki.carburoid.model.GasStationResponse
import net.canvoki.carburoid.network.GasStationApi
import net.canvoki.carburoid.product.ProductCatalogProvider

interface CountryImplementation {
    @get:StringRes
    val nameResId: Int
    val countryCode: String
    val api: GasStationApi
    val productCatalog: ProductCatalogProvider

    fun parse(json: String): GasStationResponse

    fun landMass(
        longitude: Double,
        latitude: Double,
    ): LandMass
}
