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


fun toSpanishFloat(value: Double?): String? {
    return value?.toString()?.replace(".", ",")
}
fun fromSpanishFloat(value: String?): Double? {
    return value?.replace(',', '.')?.toDoubleOrNull()
}

class SpanishFloatTypeAdapter : TypeAdapter<Double?>() {
    override fun write(out: JsonWriter, value: Double?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(toSpanishFloat(value))
        }
    }

    override fun read(`in`: JsonReader): Double? {
        val raw = `in`.nextString()
        return fromSpanishFloat(raw)
    }
}

class SpanishDateTypeAdapter : TypeAdapter<Instant?>() {
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy H:m:s", Locale.ROOT)

    override fun write(out: JsonWriter, value: Instant?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.toString()) // or format as Spanish if writing back
        }
    }

    override fun read(`in`: JsonReader): Instant? {
        val raw = `in`.nextString()
        return try {
            LocalDateTime.parse(raw, formatter).toInstant(ZoneOffset.UTC)
        } catch (e: Exception) {
            null
        }
    }
}
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
