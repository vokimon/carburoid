package net.canvoki.carburoid.location

import android.location.Location
import net.canvoki.carburoid.network.Uri

/**
 * Represents of a geographic point (WGS84).
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
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
            val coordPart =
                ssp
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
            val uri = Uri.parse(text) ?: return null

            if (uri.host == "maps.google.com") {
                // TODO: could also contain other elements
                val q = uri.getQueryParameter("q")
                return parseCoordinatesWithLabels(q)
            }

            if (uri.host != "www.google.com") {
                return null
            }

            // https://www.google.com/maps?q=lat,long
            if (uri.path == "/maps") {
                val q = uri.getQueryParameter("q")
                return parseCoordinatesWithLabels(q)
            }
            // https://www.google.com/maps/dir/40.4168,-3.7038/41.3851,2.1734/ (directions)
            if (uri.path?.startsWith("/maps/dir/") == true) {
                val points = uri.path!!.substringAfter("/maps/dir/").split("/")
                if (points.size >= 2) {
                    return parseCoordinates(points[0])
                }
            }

            // https://www.google.com/maps/place/Madrid/40.4168,-3.7038 (place)
            if (uri.path?.startsWith("/maps/place/") == true) {
                val segments = uri.path!!.substringAfter("/maps/place/").split("/")
                if (segments.size >= 2) {
                    val coords = segments[1]
                    return parseCoordinates(coords)
                }
            }

            // https://www.google.com/maps/@41.3851,2.1734,12z (Centered map)
            if (uri.path?.startsWith("/maps/@") == true) {
                val parts = uri.path!!.substringAfter("/maps/@").split(",")
                if (parts.size >= 2) {
                    val coords = "${parts[0]},${parts[1]}"
                    return parseCoordinates(coords)
                }
            }

            return null
        }

        private fun fromTextComponents(
            latString: String,
            lonString: String,
        ): GeoPoint? {
            val lat = latString.toDoubleOrNull() ?: return null
            val lon = lonString.toDoubleOrNull() ?: return null
            if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                return GeoPoint(lat, lon)
            }
            return null
        }

        private fun parseCoordinates(coordStr: String?): GeoPoint? {
            if (coordStr == null || !coordStr.contains(",")) return null
            val parts = coordStr.split(",", limit = 2)
            return fromTextComponents(parts[0], parts[1])
        }

        private fun parseCoordinatesWithLabels(input: String?): GeoPoint? {
            if (input == null) return null

            // Regex to find the LAST occurrence of "lat,lon" pattern in the string
            val regex = Regex("""([+-]?\d+(?:\.\d+)?),([+-]?\d+(?:\.\d+)?)""")
            val matches = regex.findAll(input).toList()

            // Use the last match (most likely the actual coordinates)
            if (matches.isNotEmpty()) {
                return matchToCoords(matches.last())
            }

            return null
        }

        private fun matchToCoords(match: MatchResult): GeoPoint? =
            fromTextComponents(
                match.groupValues[1],
                match.groupValues[2],
            )
    }
}
