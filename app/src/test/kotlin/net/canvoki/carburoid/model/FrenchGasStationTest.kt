package net.canvoki.carburoid.model

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.canvoki.carburoid.model.OpeningHours
import net.canvoki.shared.test.assertEquals
import net.canvoki.shared.test.assertJsonEqual
import org.junit.After
import org.junit.Before
import org.junit.Test

class FrenchGasStationTest {
    private fun assertDataEqual(
        expected: Any,
        result: Any,
    ) {
        assertEquals(expected.toString(), result.toString())
    }

    private fun baseCase(
        id: Int = 77170013,
        latitude: Double? = 48.70100,
        longitude: Double? = 2.60100,
        address: String? = "5 RUE DU GÉNÉRAL LECLERC",
        city: String? = "Brie-Comte-Robert",
        state: String? = "Seine-et-Marne",
        prices: Map<String, Double> = emptyMap(),
    ) = FrenchGasStation(
        id = id,
        name = "$id",
        brand = null,
        latitude = latitude,
        longitude = longitude,
        address = address,
        city = city,
        state = state,
        prices = prices,
        // TODO: make it french
        openingHours = OpeningHours.parse("L-D: 24H"),
    )

    // Base input JSON with all typical fields
    private fun frenchStationJson(
        id: Int = 77170013,
        latitude: String? = "4870100",
        longitude: String? = "260100",
        address: String? = "5 RUE DU GÉNÉRAL LECLERC",
        city: String? = "Brie-Comte-Robert",
        state: String? = "Seine-et-Marne",
        prices: Map<String, Double> = emptyMap(),
    ): String =
        Json.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("id", id)
                address?.let { put("adresse", it) }
                city?.let { put("ville", it) }
                state?.let { put("departement", it) }
                latitude?.let { put("latitude", it) }
                longitude?.let { put("longitude", it) }
                for ((product, price) in prices) {
                    put(product + "_prix", price)
                }
                put("horaires_jour", "Automate-24-24")
            },
        )

    @Test
    fun `French Station dumping basic case`() {
        val json = baseCase().toJson()
        assertJsonEqual(
            expected = frenchStationJson(),
            result = json,
        )
    }

    @Test
    fun `French Station parsing basic case`() {
        val json = frenchStationJson()
        val read = FrenchGasStation.parse(json)
        assertDataEqual(
            result = read,
            expected = baseCase(),
        )
    }

    @Test
    fun `French Station dump skips null fields`() {
        val json = baseCase(city = null).toJson()
        assertJsonEqual(
            expected = frenchStationJson(city = null),
            result = json,
        )
    }

    @Test
    fun `French Station parsing sets missing fields null`() {
        val json = frenchStationJson(city = null)
        val read = FrenchGasStation.parse(json)
        assertDataEqual(
            result = read,
            expected = baseCase(city = null),
        )
    }

    // Prices dict using french api ids
    val frenchPrices =
        mapOf(
            "gazole" to 1.2,
            "sp95" to 1.3,
            "sp98" to 1.4,
            "e10" to 1.5,
            "e85" to 1.6,
            "gplc" to 1.7,
        )

    // Prices dict using common ids
    val commonPrices =
        mapOf(
            "Gasoleo A" to 1.2,
            "Gasolina 95 E5" to 1.3,
            "Gasolina 98 E5" to 1.4,
            "Gasolina 95 E10" to 1.5,
            "Gasolina 95 E85" to 1.6,
            "Gases licuados del petróleo" to 1.7,
        )

    @Test
    fun `French Station dump adds _prix suffix to product prices`() {
        val json = baseCase(prices = commonPrices).toJson()
        assertJsonEqual(
            expected = frenchStationJson(prices = frenchPrices),
            result = json,
        )
    }

    @Test
    fun `French Station parsing collects _prix suffixed as product map price`() {
        val json = frenchStationJson(prices = frenchPrices)
        val read = FrenchGasStation.parse(json)
        assertDataEqual(
            result = read,
            expected = baseCase(prices = commonPrices),
        )
    }

    @Test
    fun `French Station dump just passes by unsupported products`() {
        val commonPrices = mapOf("Gasoleo A Premium" to 1.2)
        val frenchPrices = emptyMap<String, Double>()
        val json = baseCase(prices = commonPrices).toJson()
        assertJsonEqual(
            expected = frenchStationJson(prices = frenchPrices),
            result = json,
        )
    }

    @Test
    fun `French Station parsing ignores unsupported `() {
        val frenchPrices = mapOf("unsupported" to 1.2)
        val commonPrices = emptyMap<String, Double>()
        val json = frenchStationJson(prices = frenchPrices)
        val read = FrenchGasStation.parse(json)
        assertDataEqual(
            result = read,
            expected = baseCase(prices = commonPrices),
        )
    }

    @Test
    fun `name is derived from id until implemented`() {
        val station = baseCase(id = 999)
        assertEquals("999", station.name)
    }

    @Test
    fun `isPublicPrice is always true in France`() {
        val station = baseCase()
        assertEquals(true, station.isPublicPrice)
    }

    @Test
    fun `openingHours is default 24h until fully implemented`() {
        val station = baseCase()
        assertEquals("L-D: 24H", station.openingHours.toString())
    }

    @Test
    fun `timeZone is EuropeParis until fully implemented`() {
        val station = baseCase()
        assertEquals("Europe/Paris", station.timeZone().id)
    }

    @Test
    fun `product mappings are bijective`() {
        assertEquals(apiProducts.size, fromApiProduct.size)
        assertEquals(apiProducts.size, toApiProduct.size)
        for ((french, common) in apiProducts) {
            assertEquals(common, fromApiProduct[french])
            assertEquals(french, toApiProduct[common])
        }
    }

    // Response

    @Test
    fun `French response dump renames attribute stations to results`() {
        val response =
            FrenchGasStationResponse(
                stations =
                    listOf(
                        baseCase(),
                    ),
            )
        assertJsonEqual(
            expected = "[${ frenchStationJson() }]",
            result = response.toJson(),
        )
    }

    @Test
    fun `French response parse`() {
        val json = "[${ frenchStationJson() }]"
        val result = FrenchGasStationResponse.parse(json)
        assertDataEqual(
            expected = FrenchGasStationResponse(stations = listOf(baseCase())),
            result = result,
        )
    }

    // ===== FranceExtraStationData Tests =====

    private var originalExtraData: FranceExtraStationData.Db? = null

    @Before
    fun saveRestoreExtraDataState() {
        originalExtraData = FranceExtraStationData.db()
        FranceExtraStationData.clear()
    }

    @After
    fun restoreExtraDataState() {
        originalExtraData?.let { FranceExtraStationData.set(it) } ?: FranceExtraStationData.clear()
    }

    @Test
    fun `brand and name populated from extra data when ID exists in database`() =
        runBlocking {
            val tsvContent = "77170013\tTotalEnergies\tStation Centrale"
            FranceExtraStationData.load(tsvContent)

            val json = frenchStationJson(id = 77170013)
            val station = FrenchGasStation.parse(json)

            assertEquals("TotalEnergies", station.brand)
            assertEquals("Station Centrale", station.name)
        }

    @Test
    fun `brand null and name from ID when ID not in extra data database`() =
        runBlocking {
            val tsvContent = "11111111\tBrand A\tName A"
            FranceExtraStationData.load(tsvContent)

            val json = frenchStationJson(id = 99999999)
            val station = FrenchGasStation.parse(json)

            assertEquals(null, station.brand)
            assertEquals("99999999", station.name)
        }

    @Test
    fun `extra data with empty brand results in empty string brand`() =
        runBlocking {
            val tsvContent = "77170013\t\tNo Brand Station"
            FranceExtraStationData.load(tsvContent)

            val json = frenchStationJson(id = 77170013)
            val station = FrenchGasStation.parse(json)

            assertEquals("No Brand Station", station.name)
            assertEquals("", station.brand)
        }

    @Test
    fun `extra data with empty name results in brand only name`() =
        runBlocking {
            val tsvContent = "77170013\tTotalEnergies\t"
            FranceExtraStationData.load(tsvContent)

            val json = frenchStationJson(id = 77170013)
            val station = FrenchGasStation.parse(json)

            assertEquals("TotalEnergies 77170013", station.name)
            assertEquals("TotalEnergies", station.brand)
        }

    @Test
    fun `extra data with both brand and name empty results in ID as name`() =
        runBlocking {
            val tsvContent = "77170013\t\t"
            FranceExtraStationData.load(tsvContent)

            val json = frenchStationJson(id = 77170013)
            val station = FrenchGasStation.parse(json)

            assertEquals("77170013", station.name)
            assertEquals("", station.brand)
        }

    @Test
    fun `extra data load is idempotent`() =
        runBlocking {
            val tsvContent = "77170013\tTotalEnergies\tStation Centrale"
            FranceExtraStationData.load(tsvContent)
            FranceExtraStationData.load(tsvContent)

            val db = FranceExtraStationData.db()
            assertEquals(1, db?.size)
            assertEquals("TotalEnergies", db?.get("77170013")?.brand)
        }
}
