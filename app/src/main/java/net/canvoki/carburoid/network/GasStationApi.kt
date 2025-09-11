package net.canvoki.carburoid.network

import net.canvoki.carburoid.model.GasStationResponse
import retrofit2.http.GET

// https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/

interface GasStationApi {

    @GET("PreciosCarburantes/EstacionesTerrestres/")
    suspend fun getGasStations(): GasStationResponse
}

object GasStationApiFactory {
    fun create(): GasStationApi {
        return retrofit.create(GasStationApi::class.java)
    }
    private val retrofit = retrofit2.Retrofit.Builder()
        .baseUrl("https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/")
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()
}
