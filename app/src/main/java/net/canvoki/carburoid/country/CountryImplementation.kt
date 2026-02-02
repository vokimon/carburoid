package net.canvoki.carburoid.country
import net.canvoki.carburoid.network.GasStationApi

interface CountryImplementation {
    val countryCode: String
    //val api: GasStationApi
    // fun parse(json: String): GasStationResponse
    // TODO: parser, product catalog, land masses...
}
