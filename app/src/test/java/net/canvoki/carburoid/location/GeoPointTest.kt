package net.canvoki.carburoid.location

import java.util.Locale
import net.canvoki.carburoid.location.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

fun locationStr(l: GeoPoint?): String? {
    return l?.let { "GeoPoint<${"%.4f".format(Locale.ROOT, it.latitude)},${"%.4f".format(Locale.ROOT, it.longitude)}>" }
}

class GeoPointTest {
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

class GeoPoint_OpenStreetMap_Test {
    private fun testCase(text: String, expected: String?) {
        val actual = GeoPoint.fromText(text)
        assertEquals(expected, locationStr(actual))
    }

    @Test
    fun `osm link valid`() {
        testCase(
            "https://www.openstreetmap.org/#map=15/40.4168/-3.7038",
            "GeoPoint<40.4168,-3.7038>"
        )
    }

    @Test
    fun `osm link with http`() {
        testCase(
            "http://www.openstreetmap.org/#map=10/39.2114/-1.5392",
            "GeoPoint<39.2114,-1.5392>"
        )
    }

    @Test
    fun `osm link without subdomain`() {
        testCase(
            "https://openstreetmap.org/#map=12/41.3851/2.1734",
            "GeoPoint<41.3851,2.1734>"
        )
    }

    @Test
    fun `osm link negative coordinates`() {
        testCase(
            "https://www.openstreetmap.org/#map=14/-33.4567/-70.6789",
            "GeoPoint<-33.4567,-70.6789>"
        )
    }

    @Test
    fun `osm link invalid zoom level`() {
        testCase(
            "https://www.openstreetmap.org/#map=abc/40.4168/-3.7038",
            null
        )
    }

    @Test
    fun `osm link missing coordinates`() {
        testCase(
            "https://www.openstreetmap.org/#map=15/",
            null
        )
    }

    @Test
    fun `osm link extra path segments`() {
        testCase(
            "https://www.openstreetmap.org/directions?from=40.4168,-3.7038&to=41.3851,2.1734",
            // TODO: This should end-up being a path
            "GeoPoint<40.4168,-3.7038>"
        )
    }

    @Test
    fun `osm link malformed coordinates`() {
        testCase(
            "https://www.openstreetmap.org/#map=15/40.4168/",
            null
        )
    }

    @Test
    fun `osm link out of bounds latitude`() {
        testCase(
            "https://www.openstreetmap.org/#map=15/95.0/-3.7038",
            null
        )
    }

    @Test
    fun `osm link out of bounds negative latitude`() {
        testCase(
            "https://www.openstreetmap.org/#map=15/-95.0/-3.7038",
            null
        )
    }

    @Test
    fun `osm link out of bounds longitude`() {
        testCase(
            "https://www.openstreetmap.org/#map=15/40.4168/200.0",
            null
        )
    }

    @Test
    fun `osm link out of bounds negative longitude`() {
        testCase(
            "https://www.openstreetmap.org/#map=15/40.4168/-200.0",
            null
        )
    }

    @Test
    fun `rejects fake osm link`() {
        testCase(
            "https://evil.com/openstreetmap.org/#map=15/40.4168/-3.7038",
            null
        )
    }
}
