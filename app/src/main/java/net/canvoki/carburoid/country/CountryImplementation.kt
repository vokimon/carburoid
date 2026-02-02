package net.canvoki.carburoid.country

import net.canvoki.carburoid.network.GasStationApi
import net.canvoki.carburoid.network.SpainGasStationApi
import net.canvoki.carburoid.model.GasStationResponse

interface CountryImplementation {
    val countryCode: String
    val api: GasStationApi
    //fun parse(json: String) : GasStationResponse

    // TODO: product catalog, land masses...
}
