package net.canvoki.carburoid.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.canvoki.carburoid.model.OpeningHours
import org.junit.Assert.assertEquals
import org.junit.Test

private val json by lazy {
    Json {
        prettyPrint = true
    }
}

class FrenchGasStationTest {
    private fun canonicalizeJson(json: String): String {
        //print("Original:\n$json\n")
        val result = Json.parseToJsonElement(json).toString()
        //print("Canonical:\n$result\n")
        return result
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

    ): String = json.encodeToString(JsonObject.serializer(), buildJsonObject {
        put("id", id)
        address?.let { put("adresse", it) }
        city?.let { put("ville", it) }
        state?.let { put("departement", it) }
        latitude?.let { put("latitude", it) }
        longitude?.let { put("longitude", it) }
        for (product in listOf("gazole", "sp95", "e10")) {
            if (product in prices) {
                put(product + "_prix", prices[product])
            }
        }
    })

    private fun assertParseAs(json: String, expected: FrenchGasStation) {
        assertEquals(
            canonicalizeJson(json),
            canonicalizeJson(expected.toJson()),
        )
    }


    @Test
    fun `French Station parsing basic case`() {
        assertParseAs(frenchStationJson(), baseCase())
    }

    @Test
    fun `French Station parsing fields as null`() {
        assertParseAs(frenchStationJson(city = null), baseCase(city = null))
    }

    @Test
    fun `French Station parsing prices`() {
        assertParseAs(
            frenchStationJson(prices = mapOf("gazole" to 1.2)),
            baseCase(prices = mapOf("gazole" to 1.2)),
        )
    }

    @Test
    fun `French Station dumps basic case`() {
        assertEquals(
            baseCase().toJson(),
            frenchStationJson(),
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

    // Response

    private fun assertJsonEqual(result: String, expected: String) {
        assertEquals(canonicalizeJson(result), canonicalizeJson(expected))
    }

    @Test
    fun `French response`() {
        val response = FrenchGasStationResponse(
            stations = listOf(
                baseCase(),
            ),
        )
        assertJsonEqual(
            "{\"results\": [${ frenchStationJson() }]}",
            response.toJson(),
        )
    }
   

}
