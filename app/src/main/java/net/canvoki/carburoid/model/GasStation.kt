package net.canvoki.carburoid.model

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.JsonAdapter
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonWriter
import com.google.gson.stream.JsonReader
import net.canvoki.carburoid.distances.CurrentDistancePolicy

class SpanishFloatTypeAdapter : TypeAdapter<Double?>() {
    override fun write(out: JsonWriter, value: Double?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.toString().replace(".", ","))
        }
    }

    override fun read(`in`: JsonReader): Double? {
        val raw = `in`.nextString()
        return raw.replace(',', '.').toDoubleOrNull()
    }
}

data class GasStationResponse(
    @SerializedName("ListaEESSPrecio")
    val stations: List<GasStation>
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
    val priceGasoleoA: String?,

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
