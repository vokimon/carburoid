package net.canvoki.carburoid.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.canvoki.carburoid.json.fromSpanishDate
import net.canvoki.carburoid.json.postprocessSpanishNumbers
import net.canvoki.carburoid.json.preprocessSpanishNumbers
import net.canvoki.carburoid.json.toSpanishDate
import net.canvoki.shared.timeits
import java.time.Instant
import java.time.ZoneId

// JSON configuration
private val json by lazy {
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        prettyPrint = true
        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
        prettyPrintIndent = "  "
    }
}

open class NonNullableSerializer<T>(
    private val serialName: String,
    private val parse: (String) -> T,
    private val format: (T) -> String,
) : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): T {
        val s = decoder.decodeString()
        return parse(s)
    }

    override fun serialize(
        encoder: Encoder,
        value: T,
    ) {
        encoder.encodeString(format(value))
    }
}

open class NullableSerializer<T>(
    private val serialName: String,
    private val parse: (String) -> T?,
    private val format: (T) -> String,
) : KSerializer<T?> {
    override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): T? {
        val s = decoder.decodeString()
        return if (s.isBlank()) null else parse(s)
    }

    override fun serialize(
        encoder: Encoder,
        value: T?,
    ) {
        encoder.encodeString(value?.let(format) ?: "")
    }
}

object SaleTypeSerializer : NonNullableSerializer<Boolean>(
    serialName = "SaleType?",
    parse = { s -> s.uppercase() == "P" },
    format = { v -> if (v) "P" else "R" },
)

object SpanishDateTypeSerializer : NullableSerializer<Instant>(
    serialName = "Instant?",
    parse = ::fromSpanishDate,
    format = { v -> toSpanishDate(v) ?: "" },
)

object OpeningHoursSerializer : NullableSerializer<OpeningHours>(
    serialName = "OpeningHours?",
    parse = { s -> OpeningHours.parse(s) },
    format = { v -> v.toString() },
)

object NullableDoubleSerializer : NullableSerializer<Double>(
    serialName = "Double?",
    parse = { s -> s.toDoubleOrNull() },
    format = { v -> v.toString() },
)

object NullableStringSerializer : NullableSerializer<String>(
    serialName = "String?",
    parse = { it },
    format = { it },
)

@Serializable
data class SpanishGasStationResponse(
    @SerialName("ListaEESSPrecio")
    override val stations: List<
        @Serializable(with = SpanishGasStationSerializer::class)
        SpanishGasStation,
    >,
) : GasStationResponse {
    fun toJson(): String = postprocessSpanishNumbers(json.encodeToString(this))

    companion object {
        fun parse(jsonStr: String): SpanishGasStationResponse {
            val preprocessed =
                timeits("PREPROCESSAT") { preprocessSpanishNumbers(jsonStr) }
            return timeits("PARSE") {
                json.decodeFromString<SpanishGasStationResponse>(preprocessed)
            }
        }
    }
}

@Serializable
data class SpanishGasStation(
    @SerialName("IDEESS")
    override val id: Int,
    @SerialName("Rótulo")
    @Serializable(with = NullableStringSerializer::class)
    override val name: String?,
    @SerialName("Dirección")
    @Serializable(with = NullableStringSerializer::class)
    override val address: String?,
    @SerialName("Localidad")
    @Serializable(with = NullableStringSerializer::class)
    override val city: String?,
    @SerialName("Provincia")
    @Serializable(with = NullableStringSerializer::class)
    override val state: String?,
    @SerialName("Latitud")
    @Serializable(with = NullableDoubleSerializer::class)
    override val latitude: Double?,
    @SerialName("Longitud (WGS84)")
    @Serializable(with = NullableDoubleSerializer::class)
    override val longitude: Double?,
    @SerialName("Tipo Venta")
    @Serializable(with = SaleTypeSerializer::class)
    override val isPublicPrice: Boolean = true,
    @SerialName("Horario")
    @Serializable(with = OpeningHoursSerializer::class)
    override val openingHours: OpeningHours? = OpeningHours.parse("L-D: 24H"),
    @Transient
    override val prices: Map<String, Double?> = emptyMap(),
) : BaseGasStation() {
    override fun toJson(): String {
        val unprocessed = json.encodeToString(SpanishGasStationSerializer, this)
        return postprocessSpanishNumbers(unprocessed)
    }

    companion object {
        fun parse(jsonStr: String): SpanishGasStation {
            val preprocessed = preprocessSpanishNumbers(jsonStr)
            return json.decodeFromString(SpanishGasStationSerializer, preprocessed)
        }
    }
}

object SpanishGasStationSerializer : KSerializer<SpanishGasStation> {
    const val PRICE_PREFIX = "Precio "

    val delegate = SpanishGasStation.serializer()

    override val descriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): SpanishGasStation {
        val jsonDecoder = decoder as JsonDecoder
        val obj = jsonDecoder.decodeJsonElement().jsonObject

        // Extract fixed fields manually
        val id =
            obj["IDEESS"]?.jsonPrimitive?.content?.toInt()
                ?: throw IllegalArgumentException("Missing IDEESS")

        val name = obj["Rótulo"]?.jsonPrimitive?.content
        val address = obj["Dirección"]?.jsonPrimitive?.content
        val city = obj["Localidad"]?.jsonPrimitive?.content
        val state = obj["Provincia"]?.jsonPrimitive?.content

        val latitude =
            obj["Latitud"]
                ?.jsonPrimitive
                ?.content
                ?.toDoubleOrNull()

        val longitude =
            obj["Longitud (WGS84)"]
                ?.jsonPrimitive
                ?.content
                ?.toDoubleOrNull()

        val saleType = obj["Tipo Venta"]?.jsonPrimitive?.content ?: "P"
        val isPublicPrice = saleType.uppercase() == "P"

        val rawOpeningHours = obj["Horario"]?.jsonPrimitive?.content ?: "L-D: 24H"
        val openingHours = OpeningHours.parse(rawOpeningHours)

        // Extract prices
        val prices = mutableMapOf<String, Double?>()
        for ((key, value) in obj) {
            if (key.startsWith(PRICE_PREFIX)) {
                val product = key.removePrefix(PRICE_PREFIX)
                val price = value.jsonPrimitive.content.toDoubleOrNull()
                prices[product] = price
            }
        }

        return SpanishGasStation(
            id = id,
            name = name,
            address = address,
            city = city,
            state = state,
            latitude = latitude,
            longitude = longitude,
            isPublicPrice = isPublicPrice,
            openingHours = openingHours,
            prices = prices,
        )
    }

    override fun serialize(
        encoder: Encoder,
        value: SpanishGasStation,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        // Encode annotated fields
        val baseObject = jsonEncoder.json.encodeToJsonElement(delegate, value).jsonObject
        // Extract dynamic prices
        val fullObject =
            buildJsonObject {
                baseObject.forEach { (k, v) -> put(k, v) }
                value.prices.forEach { (product, price) ->
                    if (price !== null) {
                        put(PRICE_PREFIX + product, JsonPrimitive(price.toString()))
                    }
                }
            }
        jsonEncoder.encodeJsonElement(fullObject)
    }
}
