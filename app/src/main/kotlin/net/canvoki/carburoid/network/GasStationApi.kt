package net.canvoki.carburoid.network

import io.ktor.client.call.body
import io.ktor.client.request.get

interface GasStationApi {
    suspend fun getGasStations(): String
}

object SpainGasStationApi : GasStationApi {
    // https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/
    private const val ENDPOINT =
        "https://sedeaplicaciones.minetur.gob.es/" +
            "ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/"

    override suspend fun getGasStations(): String =
        Http.client
            .get(ENDPOINT)
            .body()
}

object FranceGasStationApi : GasStationApi {
    private const val ENDPOINT =
        "https://data.economie.gouv.fr/" +
            "api/explore/v2.1/catalog/" +
            "datasets/prix-des-carburants-en-france-flux-instantane-v2/" +
            // full dataset
            "exports/json?limit=-1" +
            // Remove redundant fields to reduce size
            "&select=exclude(services),exclude(prix),exclude(rupture),exclude(horaires)"

    override suspend fun getGasStations(): String =
        Http.client
            .get(ENDPOINT)
            .body()
}
