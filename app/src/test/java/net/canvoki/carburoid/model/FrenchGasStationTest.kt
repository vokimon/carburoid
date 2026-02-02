package net.canvoki.carburoid.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.canvoki.carburoid.model.OpeningHours
import net.canvoki.carburoid.test.assertJsonEqual
import org.junit.Assert.assertEquals
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
                for (product in listOf("gazole", "sp95", "e10")) {
                    if (product in prices) {
                        put(product + "_prix", prices[product])
                    }
                }
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

    @Test
    fun `French Station dump adds _prix suffix to product prices`() {
        val prices = mapOf("gazole" to 1.2)
        val json = baseCase(prices = prices).toJson()
        assertJsonEqual(
            expected = frenchStationJson(prices = prices),
            result = json,
        )
    }

    @Test
    fun `French Station parsing collects _prix suffixed as product map price`() {
        val prices = mapOf("gazole" to 1.2)
        val json = frenchStationJson(prices = prices)
        val read = FrenchGasStation.parse(json)
        assertDataEqual(
            result = read,
            expected = baseCase(prices = prices),
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
            expected = "{\"results\": [${ frenchStationJson() }]}",
            result = response.toJson(),
        )
    }

    @Test
    fun `French response parse`() {
        val json = "{\"results\": [${ frenchStationJson() }]}"
        val result = FrenchGasStationResponse.parse(json)
        assertDataEqual(
            expected = FrenchGasStationResponse(stations = listOf(baseCase())),
            result = result,
        )
    }
}
