package net.canvoki.carburoid.country

import androidx.annotation.StringRes
import net.canvoki.carburoid.R
import net.canvoki.carburoid.model.FrenchGasStationResponse
import net.canvoki.carburoid.network.FranceGasStationApi
import net.canvoki.carburoid.product.FranceProductCatalog

object FranceImplementation : CountryImplementation {
    @StringRes
    override val nameResId = R.string.settings_country_fr
    override val countryCode: String = "FR"
    override val api = FranceGasStationApi
    override val productCatalog = FranceProductCatalog

    override fun parse(json: String) = FrenchGasStationResponse.parse(json)
}
