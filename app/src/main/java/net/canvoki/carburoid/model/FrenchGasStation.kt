package net.canvoki.carburoid.model

import android.content.Context
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
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
import net.canvoki.carburoid.log
import net.canvoki.carburoid.timeits
import java.nio.charset.StandardCharsets
import java.time.ZoneId

val apiProducts =
    listOf(
        "gazole" to "Gasoleo A",
        "sp95" to "Gasolina 95 E5",
        "sp98" to "Gasolina 98 E5",
        "e10" to "Gasolina 95 E10",
        "e85" to "Gasolina 95 E85",
        "gplc" to "Gases licuados del petróleo",
    )
val fromApiProduct = apiProducts.toMap()
val toApiProduct = apiProducts.associate { (french, common) -> common to french }

// JSON configuration
private val json by lazy {
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        prettyPrint = true
    }
}

/**
 * Extra station metadata not available in French API
 */
data class FranceExtraStationData(
    val brand: String,
    val name: String,
) {
    typealias Db = Map<String, FranceExtraStationData>

    companion object {
        @Volatile
        private var extraData: Db? = null

        @VisibleForTesting
        fun clear() {
            extraData = null
        }

        fun db(): Db? = extraData

        suspend fun load(context: Context) {
            if (extraData != null) return
            try {
                val tsvContent =
                    context.assets
                        .open("fr-stations-brands-names.tsv")
                        .bufferedReader(StandardCharsets.UTF_8)
                        .use { it.readText() }
                val parsed =
                    tsvContent
                        .lines()
                        .associate { line ->
                            val parts = line.split('\t')
                            val id = parts[0]
                            val brand = parts.getOrNull(1) ?: ""
                            val name = parts.getOrNull(2) ?: ""
                            id to FranceExtraStationData(brand, name)
                        }
                extraData = parsed
                log("Loaded ${parsed.size} French station metadata entries")
            } catch (e: Exception) {
                log("Failed to load French station extra data: ${e.message}")
                extraData = emptyMap()
            }
        }
    }
}

@Serializable
data class FrenchGasStationResponse(
    override val stations: List<FrenchGasStation>,
) : GasStationResponse {
    fun toJson(): String = json.encodeToString(ListSerializer(FrenchGasStation.serializer()), stations)

    companion object {
        fun parse(jsonStr: String): FrenchGasStationResponse =
            timeits("PARSE_FRENCH") {
                val stations = json.decodeFromString<List<FrenchGasStation>>(jsonStr)
                FrenchGasStationResponse(stations)
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
        val address = obj["adresse"]?.jsonPrimitive?.content
        val city = obj["ville"]?.jsonPrimitive?.content
        val state = obj["departement"]?.jsonPrimitive?.content

        // Parse microdegrees: divide by 1e5
        val latitude =
            obj["latitude"]
                ?.jsonPrimitive
                ?.content
                ?.toDoubleOrNull()
                ?.let { it / 100000.0 }
        val longitude =
            obj["longitude"]
                ?.jsonPrimitive
                ?.content
                ?.toDoubleOrNull()
                ?.let { it / 100000.0 }

        val openingHours = obj["horaires_jour"]?.jsonPrimitive?.content?.let { FranceOpeningHours.parse(it) }
        val extraDb: FranceExtraStationData.Db? = FranceExtraStationData.db()
        val extraData: FranceExtraStationData? = extraDb?.let { it.get(id.toString()) }
        val name: String =
            when {
                extraData != null -> "${extraData.brand} - ${extraData.name}"
                else -> id.toString() // fallback
            }

        log("FR NAME $name")
        // Extract prices: any field ending with "_prix"
        val prices = mutableMapOf<String, Double?>()
        for ((key, value) in obj) {
            if (key.endsWith(PRICE_SUFFIX)) {
                val apiProduct = key.removeSuffix(PRICE_SUFFIX)
                val product = fromApiProduct[apiProduct]
                if (product == null) {
                    // TODO: Collect missing products
                    log("MISSING PRODUCT: FR $apiProduct")
                    continue
                }
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
                    if (price == null) continue
                    val apiProduct = toApiProduct[product]
                    if (apiProduct == null) continue
                    put("${apiProduct}_prix", JsonPrimitive(price))
                }
                // TODO: Proper dump of FranceOpeningHours
                value.openingHours?.let { put("horaires_jour", JsonPrimitive("Automate-24-24")) }
            }
        jsonEncoder.encodeJsonElement(obj)
    }
}
