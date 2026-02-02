package net.canvoki.carburoid.country

import net.canvoki.carburoid.model.GasStationResponse
import net.canvoki.carburoid.network.GasStationApi

interface CountryImplementation {
    val countryCode: String
    val api: GasStationApi

    fun parse(json: String): GasStationResponse

    // TODO: product catalog, land masses...
}
