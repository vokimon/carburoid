package net.canvoki.carburoid.country

object CountryRegistry {
    private const val DEFAULT_COUNTRY_CODE = "ES"

    private val implementations =
        listOf(
            SpainImplementation,
            FranceImplementation,
        )

    private val providers: Map<String, CountryImplementation> =
        implementations.associateBy { it.countryCode }

    private var _current: CountryImplementation = providers[DEFAULT_COUNTRY_CODE]!!

    var current: CountryImplementation
        get() = _current
        private set(value) {
            _current = value
        }

    fun getCountry(countryCode: String) = providers[countryCode] ?: providers[DEFAULT_COUNTRY_CODE]!!

    fun setCurrent(countryCode: String) {
        _current = getCountry(countryCode)
    }

    fun availableCountries(): List<CountryImplementation> = implementations
}
