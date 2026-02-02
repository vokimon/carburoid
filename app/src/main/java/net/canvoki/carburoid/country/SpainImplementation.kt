package net.canvoki.carburoid.country

import net.canvoki.carburoid.model.SpanishGasStationResponse
import net.canvoki.carburoid.network.SpainGasStationApi

class SpainImplementation : CountryImplementation {
    override val countryCode: String = "ES"
    override val api = SpainGasStationApi

    override fun parse(json: String) = SpanishGasStationResponse.parse(json)
}
