package net.canvoki.carburoid.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.canvoki.shared.log
import java.time.ZoneId

private val json by lazy {
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
}

/**
 * GNC density at 200 bar, 15°C (ISO 15403-1:2016, §4.2).
 * Range: 160-180 kg/m³ depending on pressure and temperature.
 * Using midpoint 170 kg/m³ for conversion from €/m³ to €/kg.
 */
private const val GNC_DENSITY_KG_PER_M3 = 170.0

private val portugalProducts =
    mapOf(
        // Diesel variants (automotive)
        "Gasóleo especial" to "Gasoleo Premium",
        "Gasóleo simples" to "Gasoleo A",
        "Gasóleo colorido" to "Gasoleo B",
        "Biodiesel B15" to "Biodiesel",
        // Gasolina variants
        "Gasolina simples 95" to "Gasolina 95 E5",
        "Gasolina especial 95" to "Gasolina 95 E5 Premium",
        "Gasolina 98" to "Gasolina 98 E5",
        //"Gasolina especial 98" to "Gasolina 98 E5 Premium", // Not a Spanish Product
        // Gases
        "GPL Auto" to "Gases licuados del petróleo",
        "GNC (gás natural comprimido) - €/kg" to "Gas Natural Comprimido",
        "GNC (gás natural comprimido) - €/m3" to "Gas Natural Comprimido", // To be normalized to €/kg
        "GNL (gás natural liquefeito) - €/kg" to "Gas Natural Licuado",
    )

@Serializable
private data class PortugalRawRecord(
    @SerialName("Id") val id: Int,
    @SerialName("Nome") val name: String?,
    @SerialName("Morada") val address: String?,
    @SerialName("Localidade") val city: String?,
    @SerialName("Distrito") val district: String?,
    @SerialName("CodPostal") val postalCode: String?,
    @SerialName("Latitude") val latitude: Double?,
    @SerialName("Longitude") val longitude: Double?,
    @SerialName("Marca") val brand: String?,
    @SerialName("Combustivel") val fuel: String?,
    @SerialName("Preco") val price: String?,
    @SerialName("DataAtualizacao") val updatedAt: String?,
)

@Serializable
private data class PortugalApiResponse(
    @SerialName("status") val status: Boolean,
    @SerialName("mensagem") val message: String?,
    @SerialName("resultado") val results: List<PortugalRawRecord>,
)

data class PortugalGasStation(
    override val id: Int,
    override val name: String?,
    override val brand: String?,
    override val address: String?,
    override val city: String?,
    override val state: String?,
    val postalCode: String?,
    override val latitude: Double?,
    override val longitude: Double?,
    override val prices: Map<String, Double?> = emptyMap(),
    override val openingHours: OpeningHours? = OpeningHours.parse("L-D: 24H"),
) : BaseGasStation() {
    override val isPublicPrice: Boolean = true

    override fun timeZone(): ZoneId = ZoneId.of("Europe/Lisbon")

    override fun toJson(): String = "{}" // TODO
}

data class PortugalGasStationResponse(
    override val stations: List<PortugalGasStation>,
) : GasStationResponse {
    companion object {
        fun parse(jsonStr: String): PortugalGasStationResponse {
            val apiResponse = json.decodeFromString<PortugalApiResponse>(jsonStr)

            val stationsById = mutableMapOf<Int, MutableList<PortugalRawRecord>>()
            for (record in apiResponse.results) {
                stationsById.getOrPut(record.id) { mutableListOf() }.add(record)
            }

            val stations =
                stationsById.values.map { records ->
                    buildStation(records)
                }

            return PortugalGasStationResponse(stations)
        }

        private fun buildStation(records: List<PortugalRawRecord>): PortugalGasStation {
            require(records.isNotEmpty()) { "Cannot create station from empty records" }
            val first = records.first()

            val prices =
                records
                    .filter { it.fuel != null && it.price != null }
                    .mapNotNull { record ->
                        val rawProduct = record.fuel!!
                        val price = parsePreco(record.price!!) ?: return@mapNotNull null

                        val normalizedPrice = normalizePrice(rawProduct, price)
                        val spanishProduct = portugalProducts[rawProduct] ?: rawProduct

                        spanishProduct to normalizedPrice
                    }.toMap()

            return PortugalGasStation(
                id = first.id,
                name = first.name,
                address = first.address,
                city = first.city,
                state = first.district,
                postalCode = first.postalCode,
                latitude = first.latitude,
                longitude = first.longitude,
                brand = first.brand,
                prices = prices,
            )
        }

        private fun normalizePrice(
            product: String,
            price: Double,
        ): Double =
            when (product) {
                "GNC (gás natural comprimido) - €/m3" -> price / GNC_DENSITY_KG_PER_M3
                else -> price
            }

        private fun parsePreco(precoStr: String): Double? =
            precoStr
                .removeSuffix("€")
                .trim()
                .replace(',', '.')
                .toDoubleOrNull()
    }
}
