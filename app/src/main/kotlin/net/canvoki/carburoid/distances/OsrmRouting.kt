package net.canvoki.carburoid.distance

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.canvoki.carburoid.distance.OsrmRouting
import net.canvoki.carburoid.network.Http
import net.canvoki.shared.log

@Serializable
private data class OsrmTableResponse(
    val distances: List<List<Double>>,
)

private val OsrmJson =
    Json {
        ignoreUnknownKeys = true
    }

class OsrmRouting {
    companion object {
        suspend fun getDistances(
            sources: List<Pair<Double, Double>>,
            destinations: List<Pair<Double, Double>>,
        ): List<List<Double>> {
            if (destinations.isEmpty()) return emptyList()
            if (sources.isEmpty()) return emptyList()

            val allCoords = sources + destinations
            // incoming as lat-lon, api expects lon-lat
            val coordString = allCoords.joinToString(";") { "${it.second},${it.first}" }

            val url = "https://router.project-osrm.org/table/v1/driving/"
            val response =
                Http.client
                    .get(url) {
                        url {
                            appendPathSegments(coordString)
                            parameters.append("sources", (0..sources.size - 1).joinToString(";"))
                            parameters.append(
                                "destinations",
                                (sources.size..sources.size + destinations.size - 1).joinToString(";"),
                            )
                            parameters.append("annotations", "distance")
                            parameters.append("skip_waypoints", "true")
                            //log("${toString()}")
                        }
                    }.body<String>()
            //log(response)
            return OsrmJson.decodeFromString<OsrmTableResponse>(response).distances
        }
    }
}
