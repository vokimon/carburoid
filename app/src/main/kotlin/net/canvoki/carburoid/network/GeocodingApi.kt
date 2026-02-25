package net.canvoki.carburoid.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import net.canvoki.shared.log
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.spatialk.geojson.Position

data class Suggestion(
    val display: String,
    val lat: Double,
    val lon: Double,
)

suspend fun searchLocation(query: String): List<Suggestion> {
    try {
        val response: String =
            Http.client
                .get("https://nominatim.openstreetmap.org/search") {
                    url {
                        parameters.append("limit", "10")
                        parameters.append("format", "json")
                        parameters.append("q", query)
                        parameters.append("countrycodes", "es") // TODO: Use current country
                    }
                }.body()
        val array = JSONArray(response)
        val newSuggestions = mutableListOf<Suggestion>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val display = obj.getString("display_name")
            val lat = obj.getDouble("lat")
            val lon = obj.getDouble("lon")
            newSuggestions.add(Suggestion(display, lat, lon))
        }
        return newSuggestions
    } catch (e: Exception) {
        log("ERROR while searching location by name '$query': $e")
        return emptyList()
    }
}

suspend fun nameLocation(position: Position): String? =
    try {
        val response: String =
            Http.client
                .get("https://nominatim.openstreetmap.org/reverse") {
                    url {
                        parameters.append("format", "json")
                        parameters.append("lat", position.latitude.toString())
                        parameters.append("lon", position.longitude.toString())
                    }
                }.body()

        val json = JSONObject(response)
        json.optString("display_name", "").takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        log("nameLocation failed for $position: $e")
        null
    }
