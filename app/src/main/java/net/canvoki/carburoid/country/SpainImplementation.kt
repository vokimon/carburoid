package net.canvoki.carburoid.country

import net.canvoki.carburoid.network.SpainGasStationApi
//import net.canvoki.carburoid.model.SpanishGasStationResponse

class SpainImplementation : CountryImplementation {
    override val countryCode: String = "ES"
    override val api = SpainGasStationApi
    //override fun parse(json: String) = SpanishGasStationResponse.parse(json)
}
