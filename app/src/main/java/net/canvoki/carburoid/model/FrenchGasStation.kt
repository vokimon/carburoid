package net.canvoki.carburoid.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.canvoki.carburoid.timeits
import java.time.ZoneId

// JSON configuration
private val json by lazy {
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        prettyPrint = true
    }
}

@Serializable
data class FrenchGasStationResponse(
    @SerialName("results")
    override val stations: List<
        @Serializable(with = FrenchGasStationSerializer::class)
        FrenchGasStation,
    >,
) : GasStationResponse {
    fun toJson(): String = json.encodeToString(FrenchGasStationResponse.serializer(), this)

    companion object {
        fun parse(jsonStr: String): FrenchGasStationResponse =
            timeits("PARSE_FRENCH") {
                json.decodeFromString<FrenchGasStationResponse>(jsonStr)
            }
    }
}

@Serializable(FrenchGasStationSerializer::class)
data class FrenchGasStation(
    override val id: Int,
    override val name: String?,
    override val latitude: Double?,
    override val longitude: Double?,
    override val address: String?,
    override val city: String?,
    override val state: String?,
    @Transient
    override val openingHours: OpeningHours?,
    @Transient
    override val prices: Map<String, Double?> = emptyMap(),
) : BaseGasStation() {
    override val isPublicPrice: Boolean = true

    override fun timeZone(): ZoneId = ZoneId.of("Europe/Paris")

    override fun toJson(): String = json.encodeToString(FrenchGasStationSerializer, this)

    companion object {
        fun parse(jsonStr: String): FrenchGasStation = json.decodeFromString(FrenchGasStationSerializer, jsonStr)
    }
}

object FrenchGasStationSerializer : KSerializer<FrenchGasStation> {
    override val descriptor = PrimitiveSerialDescriptor("FrenchGasStation", PrimitiveKind.STRING)

    private const val PRICE_SUFFIX = "_prix"

    override fun deserialize(decoder: Decoder): FrenchGasStation {
        val jsonDecoder = decoder as JsonDecoder
        val obj = jsonDecoder.decodeJsonElement().jsonObject

        // Parse ID (integer from string content)
        val id = obj["id"]?.jsonPrimitive?.content?.toInt() ?: error("Missing id")
        val name = id.toString() // TODO: Obtain name
        val address = obj["adresse"]?.jsonPrimitive?.content
        val city = obj["ville"]?.jsonPrimitive?.content
        val state = obj["departement"]?.jsonPrimitive?.content

        // Parse microdegrees: divide by 1e5
        val latitude =
            obj["latitude"]
                ?.jsonPrimitive
                ?.content
                ?.toLongOrNull()
                ?.let { it / 100000.0 }
        val longitude =
            obj["longitude"]
                ?.jsonPrimitive
                ?.content
                ?.toLongOrNull()
                ?.let { it / 100000.0 }

        // TODO: Parse french format for opening hours
        val openingHours = OpeningHours.parse("L-D: 24H")

        // Extract prices: any field ending with "_prix"
        val prices = mutableMapOf<String, Double?>()
        for ((key, value) in obj) {
            if (key.endsWith(PRICE_SUFFIX)) {
                val product = key.removeSuffix(PRICE_SUFFIX)
                val priceStr = value.jsonPrimitive.content
                val price = priceStr.toDoubleOrNull()
                prices[product] = price
            }
        }

        return FrenchGasStation(
            id = id,
            name = name,
            address = address,
            city = city,
            state = state,
            latitude = latitude,
            longitude = longitude,
            openingHours = openingHours,
            prices = prices,
        )
    }

    override fun serialize(
        encoder: Encoder,
        value: FrenchGasStation,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        val obj =
            buildJsonObject {
                put("id", JsonPrimitive(value.id))
                value.address?.let { put("adresse", JsonPrimitive(it)) }
                value.city?.let { put("ville", JsonPrimitive(it)) }
                value.state?.let { put("departement", JsonPrimitive(it)) }
                value.latitude?.let { put("latitude", JsonPrimitive((it * 100_000).toLong().toString())) }
                value.longitude?.let { put("longitude", JsonPrimitive((it * 100_000).toLong().toString())) }
                //put("horaires_jour", JsonPrimitive("L-D: 24H"))
                for ((product, price) in value.prices) {
                    if (price != null) {
                        put("${product}_prix", JsonPrimitive(price))
                    }
                }
            }
        jsonEncoder.encodeJsonElement(obj)
    }
}
