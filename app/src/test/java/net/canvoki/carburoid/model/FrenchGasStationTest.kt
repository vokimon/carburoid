package net.canvoki.carburoid.model

import kotlinx.serialization.json.Json
import net.canvoki.carburoid.model.OpeningHours
import org.junit.Assert.assertEquals
import org.junit.Test

class FrenchGasStationTest {
    // Base input JSON with all typical fields
    private fun baseInput(
        id: Int = 77170013,
        latitude: String = "4870100",
        longitude: String = "260100",
        adresse: String = "5 RUE DU GÉNÉRAL LECLERC",
        ville: String = "Brie-Comte-Robert",
        departement: String = "Seine-et-Marne",
        gazole_prix: Double? = 1.644,
        sp95_prix: Double? = null,
        e10_prix: Double? = 1.669,
    ): String =
        """
        {
          "id": $id,
          "latitude": "$latitude",
          "longitude": "$longitude",
          "adresse": "$adresse",
          "ville": "$ville",
          "departement": "$departement",
          ${if (gazole_prix != null) "\"gazole_prix\": $gazole_prix," else ""}
          ${if (sp95_prix != null) "\"sp95_prix\": $sp95_prix," else ""}
          ${if (e10_prix != null) "\"e10_prix\": $e10_prix" else ""}
        }
        """.trimIndent().removeSuffix(",")

    // Expected output after parsing + serializing
    private fun expectedOutput(
        id: Int = 77170013,
        latitude: Long = 4870100,
        longitude: Long = 260100,
        adresse: String = "5 RUE DU GÉNÉRAL LECLERC",
        ville: String = "Brie-Comte-Robert",
        departement: String = "Seine-et-Marne",
        gazole_prix: Double? = 1.644,
        sp95_prix: Double? = null,
        e10_prix: Double? = 1.669,
    ): String {
        val prices =
            buildList {
                if (gazole_prix != null) add("\"gazole_prix\":$gazole_prix")
                if (sp95_prix != null) add("\"sp95_prix\":$sp95_prix")
                if (e10_prix != null) add("\"e10_prix\":$e10_prix")
            }.joinToString(",")

        return """
            {
              "id": $id,
              "adresse": "$adresse",
              "ville": "$ville",
              "departement": "$departement",
              "latitude": $latitude,
              "longitude": $longitude,
              "horaires_jour": "L-D: 24H",
              $prices
            }
            """.trimIndent()
    }

    // Parse and re-serialize helper
    private fun parseAndSerialize(inputJson: String): String {
        val station = FrenchGasStation.parse(inputJson)
        val outputJson = station.toJson()
        // Normalize formatting for comparison
        return Json.parseToJsonElement(outputJson).toString()
    }

    @Test
    fun `parses basic station correctly`() {
        val input = baseInput()
        val expected = expectedOutput()

        val actual = parseAndSerialize(input)

        assertEquals(expected, actual)
    }

    @Test
    fun `handles null prices correctly`() {
        val input = baseInput(gazole_prix = null, e10_prix = null)
        val expected = expectedOutput(gazole_prix = null, e10_prix = null)

        val actual = parseAndSerialize(input)

        assertEquals(expected, actual)
    }

    @Test
    fun `handles missing optional fields`() {
        val input = """{ "id": 123, "latitude": "0", "longitude": "0" }"""
        val expected = """{"id":123,"latitude":0,"longitude":0,"horaires_jour":"L-D: 24H"}"""

        val actual = parseAndSerialize(input)

        assertEquals(expected, actual)
    }

    @Test
    fun `converts microdegrees correctly`() {
        val input = baseInput(latitude = "4870100", longitude = "260100")
        val expected = expectedOutput(latitude = 4870100, longitude = 260100)

        val actual = parseAndSerialize(input)

        assertEquals(expected, actual)
    }

    @Test
    fun `extracts all price fields ending with _prix`() {
        val input =
            """
            {
              "id": 1,
              "latitude": "0",
              "longitude": "0",
              "gazole_prix": 1.644,
              "sp95_prix": 1.700,
              "e10_prix": 1.669,
              "e85_prix": 0.769,
              "sp98_prix": 1.749,
              "gplc_prix": 0.890,
              "extra_field": "ignored"
            }
            """.trimIndent()

        val expected =
            """
            {
              "id":1,
              "latitude":0,
              "longitude":0,
              "horaires_jour":"L-D: 24H",
              "gazole_prix":1.644,
              "sp95_prix":1.7,
              "e10_prix":1.669,
              "e85_prix":0.769,
              "sp98_prix":1.749,
              "gplc_prix":0.89
            }
            """.trimIndent()

        val actual = parseAndSerialize(input)

        assertEquals(expected, actual)
    }

    @Test
    fun `name is derived from id`() {
        val station = FrenchGasStation.parse(baseInput(id = 999))
        assertEquals("999", station.name)
    }

    @Test
    fun `isPublicPrice is always true`() {
        val station = FrenchGasStation.parse(baseInput())
        assertEquals(true, station.isPublicPrice)
    }

    @Test
    fun `openingHours is default 24h`() {
        val station = FrenchGasStation.parse(baseInput())
        assertEquals(OpeningHours.parse("L-D: 24H"), station.openingHours)
    }

    @Test
    fun `timeZone is EuropeParis`() {
        val station = FrenchGasStation.parse(baseInput())
        assertEquals("Europe/Paris", station.timeZone().id)
    }
}
