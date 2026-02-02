package net.canvoki.carburoid.country

// TODO: Turn it into a KSP generated module
object CountryRegistry {
    private val providers: Map<String, CountryImplementation> =
        mapOf(
            "ES" to SpainImplementation(),
            "FR" to FranceImplementation(),
        )

    fun getCountry(countryCode: String): CountryImplementation = providers[countryCode] ?: providers["ES"]!!
}
