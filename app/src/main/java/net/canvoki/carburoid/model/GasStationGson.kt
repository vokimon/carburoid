package net.canvoki.carburoid.model

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import net.canvoki.carburoid.json.OpeningHoursAdapter
import net.canvoki.carburoid.json.SaleTypeAdapter
import net.canvoki.carburoid.json.SpanishDateTypeAdapter
import net.canvoki.carburoid.json.SpanishFloatTypeAdapter
import net.canvoki.carburoid.json.fromSpanishFloat
import net.canvoki.carburoid.json.postprocessSpanishNumbers
import net.canvoki.carburoid.json.preprocessSpanishNumbers
import net.canvoki.carburoid.json.toSpanishFloat
import net.canvoki.carburoid.product.ProductManager
import net.canvoki.carburoid.timeits
import java.lang.reflect.Type
import java.time.Instant
import java.time.ZoneId

private val gson: Gson by lazy {
    GsonBuilder()
        .registerTypeAdapter(
            GasStationGson::class.java,
            GasStationJsonAdapter(GsonBuilder().create()),
        ).create()
}

data class GasStationResponse(
    @SerializedName("ListaEESSPrecio")
    val stations: List<GasStationGson>,
    @SerializedName("Fecha")
    @JsonAdapter(SpanishDateTypeAdapter::class)
    val downloadDate: Instant? = null,
) {
    fun toJson(): String = postprocessSpanishNumbers(gson.toJson(this))

    companion object {
        fun parse(json: String): GasStationResponse {
            val preprocessed =
                timeits("PREPROCESSAT") {
                    preprocessSpanishNumbers(json)
                }
            return timeits("PARSE") {
                gson.fromJson(preprocessed, GasStationResponse::class.java)
            }
        }
    }
}

data class GasStationGson(
    @SerializedName("IDEESS")
    override val id: Int,
    @SerializedName("Rótulo")
    override val name: String?,
    @SerializedName("Dirección")
    override val address: String?,
    @SerializedName("Localidad")
    override val city: String?,
    @SerializedName("Provincia")
    override val state: String?,
    @SerializedName("Latitud")
    @JsonAdapter(SpanishFloatTypeAdapter::class)
    override val latitude: Double?,
    @SerializedName("Longitud (WGS84)")
    @JsonAdapter(SpanishFloatTypeAdapter::class)
    override val longitude: Double?,
    @SerializedName("Tipo Venta")
    @JsonAdapter(SaleTypeAdapter::class)
    override val isPublicPrice: Boolean = true,
    @SerializedName("Horario")
    @JsonAdapter(OpeningHoursAdapter::class)
    override val openingHours: OpeningHours? = OpeningHours.parse("L-D: 24H"),
    override val prices: Map<String, Double?> = emptyMap(),
) : BaseGasStation() {
    override fun toJson(): String = postprocessSpanishNumbers(gson.toJson(this))

    companion object {
        fun parse(json: String): GasStation = gson.fromJson(preprocessSpanishNumbers(json), GasStationGson::class.java)
    }
}

// ✅ Custom Adapter that reuses Gson’s default adapter and adds `prices` field
class GasStationJsonAdapter(
    private val gson: Gson,
) : JsonDeserializer<GasStationGson>,
    JsonSerializer<GasStationGson> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): GasStationGson {
        val jsonObject = json.asJsonObject
        // Price processing
        val prices = mutableMapOf<String, Double?>()
        for (key in jsonObject.keySet()) {
            if (key.startsWith("Precio ")) {
                val value = jsonObject.get(key)
                val price =
                    when {
                        value.isJsonNull -> null
                        value.isJsonPrimitive && value.asJsonPrimitive.isString -> fromSpanishFloat(value.asString)
                        else -> null
                    }
                val product = key.removePrefix("Precio ")
                prices[product] = price
            }
        }

        val base = gson.fromJson(jsonObject, GasStationGson::class.java)
        return base.copy(prices = prices)
    }

    override fun serialize(
        src: GasStationGson,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement {
        val jsonObject = gson.toJsonTree(src).asJsonObject

        for ((product, price) in src.prices) {
            price?.let {
                jsonObject.addProperty("Precio $product", toSpanishFloat(it))
            }
        }
        jsonObject.remove("prices")
        return jsonObject
    }
}
