package net.canvoki.carburoid.model

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.JsonAdapter
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonWriter
import com.google.gson.stream.JsonReader
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import net.canvoki.carburoid.distances.CurrentDistancePolicy
import net.canvoki.carburoid.json.SpanishDateTypeAdapter
import net.canvoki.carburoid.json.SpanishFloatTypeAdapter


data class GasStationResponse(
    @SerializedName("ListaEESSPrecio")
    val stations: List<GasStation>,

    @SerializedName("Fecha")
    @JsonAdapter(SpanishDateTypeAdapter::class)
    val downloadDate: Instant? = null,
)

fun log(message: String) {
    println("Carburoid: $message")
}

data class GasStation(
    @SerializedName("Rótulo")
    val name: String?,

    @SerializedName("Dirección")
    val address: String?,

    @SerializedName("Localidad")
    val city: String?,

    @SerializedName("Provincia")
    val state: String?,

    @SerializedName("Precio Gasoleo A")
    @JsonAdapter(SpanishFloatTypeAdapter::class)
    val priceGasoleoA: Double?,

    @SerializedName("Latitud")
    @JsonAdapter(SpanishFloatTypeAdapter::class)
    val latitude: Double?,

    @SerializedName("Longitud (WGS84)")
    @JsonAdapter(SpanishFloatTypeAdapter::class)
    val longitude: Double?,

) {
    var distanceInMeters: Float? = null
        private set  // use computeDistance

    fun computeDistance() {
        distanceInMeters = CurrentDistancePolicy.getDistance(this)
    }
}
