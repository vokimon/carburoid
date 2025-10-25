package net.canvoki.carburoid.location

import android.location.Location

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
            val lat = coords[0].toDoubleOrNull() ?: return null
            val lon = coords[1].toDoubleOrNull() ?: return null
            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
            return GeoPoint(lat, lon)
        }

        /** Parse from free-form text (decimal, DMS, OSM links, etc.) */
        fun fromText(text: String?): GeoPoint? {
            if (text == null) return null
            return fromGeoUri(text)
                ?: parseOsmLink(text)
                ?: parseDecimalCoords(text)
        }


        private fun parseDecimalCoords(text: String): GeoPoint? {
            // Match: "40.4168, -3.7038" or "40.4168,-3.7038"
            val regex = Regex("""([+-]?\d+(?:\.\d+)?)[,\s]+([+-]?\d+(?:\.\d+)?)""")
            val match = regex.find(text) ?: return null
            val lat = match.groupValues[1].toDoubleOrNull() ?: return null
            val lon = match.groupValues[2].toDoubleOrNull() ?: return null
            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
            return GeoPoint(lat, lon)
        }

        private fun parseOsmLink(text: String): GeoPoint? {
            // Match: https://www.openstreetmap.org/#map=15/40.4168/-3.7038
            val regex = Regex("""https?://(?:www\.)?openstreetmap\.org/#map=\d+/([+-]?\d+(?:\.\d+)?)/([+-]?\d+(?:\.\d+)?)""")
            val match = regex.find(text) ?: return null
            val lat = match.groupValues[1].toDoubleOrNull() ?: return null
            val lon = match.groupValues[2].toDoubleOrNull() ?: return null
            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
            return GeoPoint(lat, lon)
        }
    }
}
