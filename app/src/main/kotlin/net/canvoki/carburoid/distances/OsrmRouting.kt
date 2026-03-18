package net.canvoki.carburoid.distances

import com.google.common.util.concurrent.RateLimiter
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.canvoki.carburoid.location.GeoPoint
import net.canvoki.carburoid.network.Http
import net.canvoki.shared.log

@Serializable
private data class OsrmTableResponse(
    val distances: List<List<Double>>,
)

// The API delays calls if they come from the same source.
// Results are better if spaced minimum two seconds (0.5 per second)
// If not, latencies get up to 8 seconds.
val maxCallsPerSecond = 1.0
val rateLimiter = RateLimiter.create(maxCallsPerSecond)

private val OsrmJson =
    Json {
        ignoreUnknownKeys = true
    }

class OsrmRouting {
    companion object {
        suspend fun getDistances(
            sources: List<GeoPoint>,
            destinations: List<GeoPoint>,
        ): List<List<Double>> {
            if (destinations.isEmpty()) return emptyList()
            if (sources.isEmpty()) return emptyList()

            val allCoords = sources + destinations
            // incoming as lat-lon, api expects lon-lat
            val coordString = allCoords.joinToString(";") { "${it.longitude},${it.latitude}" }

            rateLimiter.acquire()

            val url = "https://router.project-osrm.org/table/v1/driving/"
            var fullUrl = ""
            try {
                val response =
                    Http.client
                        .get(url) {
                            url {
                                appendPathSegments(coordString)
                                encodedParameters.append("sources", (0..sources.size - 1).joinToString(";"))
                                encodedParameters.append(
                                    "destinations",
                                    (sources.size..sources.size + destinations.size - 1).joinToString(";"),
                                )
                                parameters.append("annotations", "distance")
                                parameters.append("skip_waypoints", "true")
                                fullUrl = toString()
                            }
                            timeout {
                                socketTimeoutMillis = 100_000
                            }
                        }.body<String>()
                try {
                    //log("${fullUrl}")
                    //log(response)
                    return OsrmJson.decodeFromString<OsrmTableResponse>(response).distances
                } catch (e: Exception) {
                    log("Error: $fullUrl")
                    log("Error $response")
                    return emptyList()
                }
            } catch (e: Exception) {
                log("$fullUrl")
                log("Error: $e")
                //log(response)
                return emptyList()
            }
        }
    }
}
