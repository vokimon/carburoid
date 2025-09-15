package net.canvoki.carburoid.model

import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.JsonAdapter
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.Instant
import net.canvoki.carburoid.distances.CurrentDistancePolicy
import net.canvoki.carburoid.json.SpanishDateTypeAdapter
import net.canvoki.carburoid.json.SpanishFloatTypeAdapter
import net.canvoki.carburoid.json.toSpanishFloat
import net.canvoki.carburoid.json.fromSpanishFloat


// ✅ GasStationResponse with parser using the enhanced Gson
data class GasStationResponse(
    @SerializedName("ListaEESSPrecio")
    val stations: List<GasStation>,

    @SerializedName("Fecha")
    @JsonAdapter(SpanishDateTypeAdapter::class)
    val downloadDate: Instant? = null
) {
    companion object {
        private val gson: Gson by lazy {
            GsonBuilder()
                .registerTypeAdapter(
                    GasStation::class.java,
                    GasStationJsonAdapter(GsonBuilder().create()) // inject safe Gson
                )
                .create()
        }

        fun parse(json: String): GasStationResponse {
            return gson.fromJson(json, GasStationResponse::class.java)
        }
    }
}

// ✅ GasStation data class
data class GasStation(
    @SerializedName("Rótulo")
    val name: String?,

    @SerializedName("Dirección")
    val address: String?,

    @SerializedName("Localidad")
    val city: String?,

    @SerializedName("Provincia")
    val state: String?,

    @SerializedName("Latitud")
    @JsonAdapter(SpanishFloatTypeAdapter::class)
    val latitude: Double?,

    @SerializedName("Longitud (WGS84)")
    @JsonAdapter(SpanishFloatTypeAdapter::class)
    val longitude: Double?,

    val prices: Map<String, Double?> = emptyMap()
) {
    var distanceInMeters: Float? = null
        private set

    fun computeDistance() {
        distanceInMeters = CurrentDistancePolicy.getDistance(this)
    }

    val price: Double?
        get() = prices[currentProduct]  // For now

    companion object {

        val DEFAULT_PRODUCT = "Gasoleo A"
        private var currentProduct : String = DEFAULT_PRODUCT
        fun setCurrentProduct(product: String) {
            currentProduct = product
        }
        fun resetCurrentProduct() {
            currentProduct = DEFAULT_PRODUCT
        }

        private val gson: Gson by lazy {
            GsonBuilder()
                .registerTypeAdapter(
                    GasStation::class.java,
                    GasStationJsonAdapter(GsonBuilder().create())
                )
                .create()
        }

        fun parse(json: String): GasStation {
            return gson.fromJson(json, GasStation::class.java)
        }
    }
}

// ✅ Custom Adapter that reuses Gson’s default adapter and adds `prices` field
class GasStationJsonAdapter(
    private val gson: Gson
) : JsonDeserializer<GasStation>, JsonSerializer<GasStation> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): GasStation {
        val jsonObject = json.asJsonObject

        // ✅ Use Gson’s default adapter (no recursion)
        val delegate = gson.getDelegateAdapter(null, object : TypeToken<GasStation>() {})
        val base = delegate.fromJsonTree(jsonObject)

        // ✅ Extract dynamic price fields
        val prices = jsonObject.entrySet()
            .filter { (key, _) -> key.startsWith("Precio ") }
            .associate { (key, value) ->
                val product = key.removePrefix("Precio ")
                val price = fromSpanishFloat(value.asString)
                product to price
            }

        return base.copy(prices = prices)
    }

    override fun serialize(
        src: GasStation,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        // ✅ Use default serialization
        val jsonObject = gson.toJsonTree(src).asJsonObject

        // ✅ Add dynamic price fields
        for ((product, price) in src.prices) {
            price?.let {
                jsonObject.addProperty("Precio $product", toSpanishFloat(it))
            }
        }

        return jsonObject
    }
}
