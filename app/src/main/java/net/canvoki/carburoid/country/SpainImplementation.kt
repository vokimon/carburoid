package net.canvoki.carburoid.country

import net.canvoki.carburoid.model.SpanishGasStationResponse
import net.canvoki.carburoid.network.SpainGasStationApi
import net.canvoki.carburoid.product.SpainProductCatalog

object SpainImplementation : CountryImplementation {
    override val countryCode: String = "ES"
    override val api = SpainGasStationApi
    override val productCatalog = SpainProductCatalog

    override fun parse(json: String) = SpanishGasStationResponse.parse(json)
}
