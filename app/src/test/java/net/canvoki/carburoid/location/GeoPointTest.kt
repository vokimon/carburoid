package net.canvoki.carburoid.location

import java.util.Locale
import net.canvoki.carburoid.location.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoPointTest {

    private fun locationStr(l: GeoPoint?): String? {
        return l?.let { "GeoPoint<${"%.4f".format(Locale.ROOT, it.latitude)},${"%.4f".format(Locale.ROOT, it.longitude)}>" }
    }

    private fun testGeoUri(uri: String, expected: String?) {
        val actual = GeoPoint.fromGeoUri(uri)
        assertEquals(expected, locationStr(actual))
    }

    @Test
    fun `simple valid geo uri`() {
        testGeoUri("geo:40.4168,-3.7038", "GeoPoint<40.4168,-3.7038>")
    }

    @Test
    fun `non-geo scheme returns null`() {
        testGeoUri("https://example.com", null)
    }

    @Test
    fun `no comma returns null`() {
        testGeoUri("geo:40.4168", null)
    }

    @Test
    fun `extra comma-separated values ignored`() {
        testGeoUri("geo:40.4168,-3.7038,100.0", "GeoPoint<40.4168,-3.7038>")
    }

    @Test
    fun `bad latitude`() {
        testGeoUri("geo:bad,-3.7038", null)
    }

    @Test
    fun `bad longitude`() {
        testGeoUri("geo:40.4168,bad", null)
    }

    @Test
    fun `non-float latitude returns null`() {
        testGeoUri("geo:invalid,-3.7038", null)
    }

    @Test
    fun `non-float longitude returns null`() {
        testGeoUri("geo:40.4168,invalid", null)
    }

    @Test
    fun `valid geo uri with query parameters`() {
        testGeoUri("geo:40.4168,-3.7038?z=100", "GeoPoint<40.4168,-3.7038>")
    }

    @Test
    fun `valid geo uri with crs parameter`() {
        testGeoUri("geo:40.4168,-3.7038;crs=wgs84", "GeoPoint<40.4168,-3.7038>")
    }

    @Test
    fun `geo uri with leading slash`() {
        testGeoUri("geo:/40.4168,-3.7038", "GeoPoint<40.4168,-3.7038>")
    }

    @Test
    fun `latitude out of bounds returns null`() {
        testGeoUri("geo:91.0,-3.7038", null)
        testGeoUri("geo:-91.0,-3.7038", null)
    }

    @Test
    fun `longitude out of bounds returns null`() {
        testGeoUri("geo:40.4168,181.0", null)
        testGeoUri("geo:40.4168,-181.0", null)
    }

    @Test
    fun `whitespace in coordinates handled`() {
        testGeoUri("geo:  40.4168  ,  -3.7038  ", "GeoPoint<40.4168,-3.7038>")
    }

    @Test
    fun `empty uri returns null`() {
        testGeoUri("", null)
    }

    @Test
    fun `geo uri with empty coordinates returns null`() {
        testGeoUri("geo:,", null)
    }
}
