package net.canvoki.carburoid.country

import net.canvoki.carburoid.network.FranceGasStationApi
import net.canvoki.carburoid.model.FrenchGasStationResponse

class FranceImplementation : CountryImplementation {
    override val countryCode: String = "FR"
    override val api = FranceGasStationApi
    override fun parse(json: String) = FrenchGasStationResponse.parse(json)
}
