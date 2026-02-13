package net.canvoki.carburoid.location

import net.canvoki.shared.test.assertEquals
import org.junit.Test
import java.util.Locale

fun locationStr(l: GeoPoint?): String? =
    l?.let {
        "GeoPoint<${"%.4f".format(Locale.ROOT, it.latitude)},${"%.4f".format(Locale.ROOT, it.longitude)}>"
    }

class GeoPointTest {
    ////////////////////////////////////////////////////////////////////////////////
    // Geo URI (geo:...)

    private fun testGeoUri(
        uri: String,
        expected: String?,
    ) {
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

    ////////////////////////////////////////////////////////////////////////////////
    // Open Street Maps (OSM)

    private fun testOsmLink(
        text: String,
        expected: String?,
    ) {
        val actual = GeoPoint.fromOsmLink(text)
        assertEquals(expected, locationStr(actual))
    }

    @Test
    fun `osm link valid`() {
        testOsmLink(
            "https://www.openstreetmap.org/#map=15/40.4168/-3.7038",
            "GeoPoint<40.4168,-3.7038>",
        )
    }

    @Test
    fun `osm link with http`() {
        testOsmLink(
            "http://www.openstreetmap.org/#map=10/39.2114/-1.5392",
            "GeoPoint<39.2114,-1.5392>",
        )
    }

    @Test
    fun `osm link without subdomain`() {
        testOsmLink(
            "https://openstreetmap.org/#map=12/41.3851/2.1734",
            "GeoPoint<41.3851,2.1734>",
        )
    }

    @Test
    fun `osm link negative coordinates`() {
        testOsmLink(
            "https://www.openstreetmap.org/#map=14/-33.4567/-70.6789",
            "GeoPoint<-33.4567,-70.6789>",
        )
    }

    @Test
    fun `osm link invalid zoom level`() {
        testOsmLink(
            "https://www.openstreetmap.org/#map=abc/40.4168/-3.7038",
            null,
        )
    }

    @Test
    fun `osm link missing coordinates`() {
        testOsmLink(
            "https://www.openstreetmap.org/#map=15/",
            null,
        )
    }

    // TODO: This should end-up being a path
    @Test
    fun `osm link directions takes origin`() {
        testOsmLink(
            "https://www.openstreetmap.org/directions?from=40.4168,-3.7038&to=41.3851,2.1734",
            "GeoPoint<40.4168,-3.7038>",
        )
    }

    @Test
    fun `osm link malformed coordinates`() {
        testOsmLink(
            "https://www.openstreetmap.org/#map=15/40.4168/",
            null,
        )
    }

    @Test
    fun `osm link out of bounds latitude`() {
        testOsmLink(
            "https://www.openstreetmap.org/#map=15/95.0/-3.7038",
            null,
        )
    }

    @Test
    fun `osm link out of bounds negative latitude`() {
        testOsmLink(
            "https://www.openstreetmap.org/#map=15/-95.0/-3.7038",
            null,
        )
    }

    @Test
    fun `osm link out of bounds longitude`() {
        testOsmLink(
            "https://www.openstreetmap.org/#map=15/40.4168/200.0",
            null,
        )
    }

    @Test
    fun `osm link out of bounds negative longitude`() {
        testOsmLink(
            "https://www.openstreetmap.org/#map=15/40.4168/-200.0",
            null,
        )
    }

    @Test
    fun `rejects fake osm link`() {
        testOsmLink(
            "https://evil.com/openstreetmap.org/#map=15/40.4168/-3.7038",
            null,
        )
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Google Maps

    private fun testGMapsUri(
        text: String,
        expected: String?,
    ) {
        val actual = GeoPoint.fromGoogleMapsLink(text)
        assertEquals(expected, locationStr(actual))
    }

    // Pattern: https://maps.google.coom?q=lat,long

    @Test
    fun `gmaps subdomain`() {
        testGMapsUri(
            "https://maps.google.com/?q=40.4168,-3.7038",
            "GeoPoint<40.4168,-3.7038>",
        )
    }

    @Test
    fun `gmaps subdomain with extra params ignored`() {
        testGMapsUri(
            "https://maps.google.com/?q=41.3851,2.1734&z=15",
            "GeoPoint<41.3851,2.1734>",
        )
    }

    @Test
    fun `gmaps subdomain integer`() {
        testGMapsUri(
            "https://maps.google.com/?q=40,-3",
            "GeoPoint<40.0000,-3.0000>",
        )
    }

    @Test
    fun `gmaps subdomain invalid latitude`() {
        testGMapsUri(
            "https://maps.google.com/?q=bad,3",
            null,
        )
    }

    @Test
    fun `gmaps subdomain invalid longitude`() {
        testGMapsUri(
            "https://maps.google.com/?q=40,bad",
            null,
        )
    }

    @Test
    fun `gmaps subdomain latitude to high`() {
        testGMapsUri(
            "https://maps.google.com/?q=95.0,-3.7038",
            null,
        )
    }

    @Test
    fun `gmaps subdomain latitude too low`() {
        testGMapsUri(
            "https://maps.google.com/?q=-95.0,-3.7038",
            null,
        )
    }

    @Test
    fun `gmaps subdomain longitude too high`() {
        testGMapsUri(
            "https://maps.google.com/?q=40.4168,200.0",
            null,
        )
    }

    @Test
    fun `gmaps subdomain longitude too low`() {
        testGMapsUri(
            "https://maps.google.com/?q=40.4168,-200.0",
            null,
        )
    }

    @Test
    fun `gmaps subdomain missing q param`() {
        testGMapsUri(
            "https://maps.google.com/?z=15",
            null,
        )
    }

    @Test
    fun `gmaps subdomain with label and coords`() {
        testGMapsUri(
            "https://maps.google.com/?q=Madrid%2040.4168,-3.7038",
            "GeoPoint<40.4168,-3.7038>",
        )
    }

    @Test
    fun `gmaps subdomain with parentheses`() {
        testGMapsUri(
            "https://maps.google.com/?q=Station+(40.4168,-3.7038)",
            "GeoPoint<40.4168,-3.7038>",
        )
    }

    // Pattern: www.google.com/maps?q=lat,long

    @Test
    fun `gmaps path`() {
        testGMapsUri(
            "https://www.google.com/maps?q=39.2114,-1.5392",
            "GeoPoint<39.2114,-1.5392>",
        )
    }

    @Test
    fun `gmaps path but no google url`() {
        testGMapsUri(
            "https://example.com/maps?q=40.4168,-3.7038",
            null,
        )
    }

    @Test
    fun `gmaps path bad lat`() {
        testGMapsUri(
            "https://www.google.com/maps?q=bad,-3.7038",
            null,
        )
    }

    @Test
    fun `gmaps path lat too low`() {
        testGMapsUri(
            "https://www.google.com/maps?q=-95.0,-3.7038",
            null,
        )
    }

    @Test
    fun `gmaps path with label and coords`() {
        testGMapsUri(
            "https://www.google.com/maps?q=Madrid%2040.4168,-3.7038",
            "GeoPoint<40.4168,-3.7038>",
        )
    }

    @Test
    fun `gmaps path with parentheses`() {
        testGMapsUri(
            "https://www.google.com/maps?q=Station+(40.4168,-3.7038)",
            "GeoPoint<40.4168,-3.7038>",
        )
    }

    // Pattern: https://www.google.com/maps/dir/lat1,lon1/lat2,lon2/...

    @Test
    fun `gmaps dir takes route origin`() {
        testGMapsUri(
            "https://www.google.com/maps/dir/40.4168,-3.7038/41.3851,2.1734/",
            "GeoPoint<40.4168,-3.7038>",
        )
    }

    @Test
    fun `gmaps dir incomplete`() {
        testGMapsUri(
            "https://www.google.com/maps/dir/40.4168/",
            null,
        )
    }

    @Test
    fun `gmaps dir with multiple waypoints`() {
        testGMapsUri(
            "https://www.google.com/maps/dir/39.2114,-1.5392/40.4168,-3.7038/41.3851,2.1734/42.8467,-1.2345/",
            "GeoPoint<39.2114,-1.5392>",
        )
    }

    @Test
    fun `gmaps dir invalid number latitude`() {
        testGMapsUri(
            "https://www.google.com/maps/dir/6.6.6,-3.7038/41.3851,2.1734/",
            null,
        )
    }

    @Test
    fun `gmaps dir invalid number longitude`() {
        testGMapsUri(
            "https://www.google.com/maps/dir/40.4168,6.6.6/41.3851,2.1734/",
            null,
        )
    }

    @Test
    fun `gmaps dir lat too low`() {
        testGMapsUri(
            "https://www.google.com/maps/dir/-95.0,-3.7038/41.3851,2.1734/",
            null,
        )
    }

    // === Pattern 3: /maps/place/... ===
    @Test
    fun `gmaps place with coords valid`() {
        testGMapsUri(
            "https://www.google.com/maps/place/Abengibre/39.2114,-1.5392",
            "GeoPoint<39.2114,-1.5392>",
        )
    }

    @Test
    fun `gmaps place bad lon`() {
        testGMapsUri(
            "https://www.google.com/maps/place/Barcelona/41.3851,6.6.6",
            null,
        )
    }

    @Test
    fun `gmaps place lon too low`() {
        testGMapsUri(
            "https://www.google.com/maps/place/Sevilla/37.3891,-200.0",
            null,
        )
    }

    // === Pattern 4: /maps/@... ===

    @Test
    fun `gmaps map view center valid`() {
        testGMapsUri(
            "https://www.google.com/maps/@41.3851,2.1734,12z",
            "GeoPoint<41.3851,2.1734>",
        )
    }

    @Test
    fun `gmaps map view bad lat`() {
        testGMapsUri(
            "https://www.google.com/maps/@6.6.6,2.1734,12z",
            null,
        )
    }

    @Test
    fun `gmaps map view lon too low`() {
        testGMapsUri(
            "https://www.google.com/maps/@41.3851,-200.0,12z",
            null,
        )
    }
}
