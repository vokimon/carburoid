package net.canvoki.carburoid.country

import net.canvoki.carburoid.test.assertEquals
import org.junit.Test

class CountryRegistryTest {
    @Test
    fun `getting ES returns ES implementation`() {
        val country = CountryRegistry.getCountry("ES")
        assertEquals("ES", country.countryCode)
    }

    @Test
    fun `getting unknown country falls back to ES`() {
        val country = CountryRegistry.getCountry("NOCOUNTRY")
        assertEquals("ES", country.countryCode)
    }

    @Test
    fun `getting FR returns FR`() {
        val country = CountryRegistry.getCountry("FR")
        assertEquals("FR", country.countryCode)
    }
}
