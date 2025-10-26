package net.canvoki.carburoid.location

import android.location.Location
import net.canvoki.carburoid.network.Uri

/**
 * Represents of a geographic point (WGS84).
 */
data class GeoPoint(val latitude: Double, val longitude: Double) {

    fun toAndroidLocation(): Location {
        val gp = this
        return Location("carburoid").apply {
            latitude = gp.latitude
            longitude = gp.longitude
        }
    }

    companion object {

        /** Parse from free-form text (decimal, DMS, OSM links, etc.) */
        fun fromText(text: String?): GeoPoint? {
            if (text == null) return null
            return fromGeoUri(text)
                ?: fromOsmLink(text)
                ?: fromDecimalCoords(text)
        }

        /** Parse from geo: URI string (RFC 5870) */
        fun fromGeoUri(uriString: String?): GeoPoint? {
            if (uriString == null || !uriString.startsWith("geo:", ignoreCase = true)) {
                return null
            }
            val ssp = uriString.substringAfter("geo:", "").removePrefix("/")
            val coordPart = ssp
                .substringBefore(';')
                .substringBefore('?')
                .trim()
            val coords = coordPart.split(',').map { it.trim() }
            if (coords.size < 2) return null
            return fromTextComponents(coords[0], coords[1])
        }

        fun fromDecimalCoords(text: String): GeoPoint? {
            // Match: "40.4168, -3.7038" or "40.4168,-3.7038"
            val regex = Regex("""([+-]?\d+(?:\.\d+)?)[,\s]+([+-]?\d+(?:\.\d+)?)""")
            val match = regex.find(text) ?: return null
            return fromTextComponents(match.groupValues[1], match.groupValues[2])
        }

        fun fromOsmLink(text: String): GeoPoint? {
            val uri = Uri.parse(text) ?: return null

            if (uri.host != "www.openstreetmap.org" && uri.host != "openstreetmap.org") {
                return null
            }

            if (uri.fragment?.startsWith("map=") == true) {
                val parts = uri.fragment!!.substringAfter("map=").split("/")
                if (parts.size >= 3 && parts[0].toDoubleOrNull() !== null) {
                    return fromTextComponents(parts[1], parts[2])
                }
            }

            if (uri.path == "/directions") {
                val fromParam = uri.getQueryParameter("from")
                if (fromParam != null && fromParam.contains(",")) {
                    val coords = fromParam.split(",", limit = 2)
                    if (coords.size == 2) {
                        return fromTextComponents(coords[0], coords[1])
                    }
                }
            }

            return null
         }

         fun fromGoogleMapsLink(text: String): GeoPoint? {
            // https://maps.google.com/?q=40.4168,-3.7038 (subdomain)
            Regex("""https?://maps\.google\.com/\?(?:[^#]*?[&?])?q=([+-]?\d+(?:\.\d+)?),([+-]?\d+(?:\.\d+)?)""")
                .find(text)?.let { match ->
                    return matchToCoords(match)
                }

            // https://www.google.com/maps/dir/40.4168,-3.7038/41.3851,2.1734/ (directions)
            Regex("""https?://(?:www\.)?google\.com/maps/dir/([+-]?\d+(?:\.\d+)?),([+-]?\d+(?:\.\d+)?)/""")
                .find(text)?.let { match ->
                    return matchToCoords(match)
                }

            // https://www.google.com/maps/place/Madrid/40.4168,-3.7038 (place)
            Regex("""https?://(?:www\.)?google\.com/maps/place/[^/]+/([+-]?\d+(?:\.\d+)?),([+-]?\d+(?:\.\d+)?)""")
                .find(text)?.let { match ->
                    return matchToCoords(match)
                }

            // https://www.google.com/maps/@40.4168,-3.7038 (at)
            Regex("""https?://(?:www\.)?google\.com/maps/@([+-]?\d+(?:\.\d+)?),([+-]?\d+(?:\.\d+)?),\d+z?""")
                .find(text)?.let { match ->
                    return matchToCoords(match)
                }

            // https://www.google.com/maps?q=40.4168,-3.7038 (path & query)
            Regex("""https?://(?:www\.)?google\.com/maps\?(?:[^#]*?[&?])?q=([+-]?\d+(?:\.\d+)?),([+-]?\d+(?:\.\d+)?)""")
                .find(text)?.let { match ->
                    return matchToCoords(match)
                }

            return null
        }

        private fun matchToCoords(match: MatchResult): GeoPoint? {
            return fromTextComponents(match.groupValues[1], match.groupValues[2])
        }

        private fun fromTextComponents(latString: String, lonString: String): GeoPoint? {
            val lat = latString.toDoubleOrNull() ?: return null
            val lon = lonString.toDoubleOrNull() ?: return null
            if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                return GeoPoint(lat, lon)
            }
            return null
        }
    }
}
