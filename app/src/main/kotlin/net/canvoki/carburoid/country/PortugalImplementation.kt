package net.canvoki.carburoid.country

import androidx.annotation.StringRes
import net.canvoki.carburoid.R
import net.canvoki.carburoid.distances.LandMass
import net.canvoki.carburoid.distances.PortugalLandMass
import net.canvoki.carburoid.model.PortugalGasStationResponse
import net.canvoki.carburoid.network.PortugalGasStationApi
import net.canvoki.carburoid.product.PortugalProductCatalog

object PortugalImplementation : CountryImplementation {
    @StringRes
    override val nameResId = R.string.settings_country_pt
    override val countryCode: String = "PT"
    override val api = PortugalGasStationApi
    override val productCatalog = PortugalProductCatalog

    override fun parse(json: String) = PortugalGasStationResponse.parse(json)

    override fun landMass(
        longitude: Double,
        latitude: Double,
    ): LandMass = PortugalLandMass.of(longitude, latitude)
}
