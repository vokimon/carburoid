package net.canvoki.carburoid.country

import androidx.annotation.StringRes
import net.canvoki.carburoid.R
import net.canvoki.carburoid.distances.LandMass
import net.canvoki.carburoid.distances.SpainLandMass
import net.canvoki.carburoid.model.SpanishGasStationResponse
import net.canvoki.carburoid.network.SpainGasStationApi
import net.canvoki.carburoid.product.SpainProductCatalog

object SpainImplementation : CountryImplementation {
    @StringRes
    override val nameResId = R.string.settings_country_es
    override val countryCode: String = "ES"
    override val api = SpainGasStationApi
    override val productCatalog = SpainProductCatalog

    override fun parse(json: String) = SpanishGasStationResponse.parse(json)

    override fun landMass(
        longitude: Double,
        latitude: Double,
    ): LandMass = SpainLandMass.of(longitude, latitude)
}
