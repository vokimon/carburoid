package net.canvoki.carburoid.model

import net.canvoki.shared.test.assertEquals
import org.junit.Test

class PortugalGasStationTest {
    /**
     * Builds a single station record JSON.
     * Meaningful params: id, product, price
     * Other fields are derived from id for consistency.
     */
    private fun stationJson(
        id: Int,
        product: String,
        price: String,
        name: String = "Station $id",
        brand: String = "Brand $id",
        city: String = "Lisboa",
        state: String = "Lisboa",
        latitude: Double = 38.7 + id * 0.1,
        longitude: Double = -9.1 + id * 0.1,
    ): String =
        """{"Id":$id,"Nome":"$name","Marca":"$brand",""" +
            """"Morada":"Street $id","Localidade":"$city","Distrito":"$state",""" +
            """"CodPostal":"1000-001","Latitude":$latitude,"Longitude":$longitude,""" +
            """"Combustivel":"$product","Preco":"$price €","DataAtualizacao":"2026-03-25 00:00"}"""

    /**
     * Wraps a list of station JSONs into the full API response.
     */
    private fun portugalApiResponseJson(stations: List<String>): String =
        """{"status":true,"mensagem":"sucesso","resultado":[${stations.joinToString(",")}]}"""

    // ===== Test Helpers =====

    private fun buildStationSummary(response: PortugalGasStationResponse): String {
        val sb = StringBuilder()
        sb.appendLine("Stations: ${response.stations.size}")
        for (station in response.stations.sortedBy { it.id }) {
            sb.appendLine("Station[${station.id}]: name=${station.name}, city=${station.city}, brand=${station.brand}")
            station.prices
                .filterValues { it != null }
                .toList()
                .sortedBy { it.first }
                .map { "${it.first}=${it.second}" }
                .forEach { sb.appendLine("  $it") }
        }
        return sb.toString().trimEnd()
    }

    /**
     * Parses input JSON and asserts the station summary matches expected output.
     * Combines parsing, summary building, and assertion in one call.
     */
    private fun assertParsedStations(
        input: String,
        expected: String,
    ) {
        val actual = buildStationSummary(PortugalGasStationResponse.parse(input))
        assertEquals(expected, actual)
    }

    // ===== Tests =====

    @Test
    fun `maps portuguese diesel to spanish product name`() {
        val input = portugalApiResponseJson(listOf(stationJson(id = 1, product = "Gasóleo simples", price = "1,500")))

        val expected =
            """
            Stations: 1
            Station[1]: name=Station 1, city=Lisboa, brand=Brand 1
              Gasoleo A=1.5
            """.trimIndent()

        assertParsedStations(input, expected)
    }

    @Test
    fun `merges multiple products for same station id`() {
        val input =
            portugalApiResponseJson(
                listOf(
                    stationJson(id = 1, product = "Gasóleo simples", price = "1,500"),
                    stationJson(id = 1, product = "Gasolina 95 E5", price = "1,700"),
                ),
            )

        val expected =
            """
            Stations: 1
            Station[1]: name=Station 1, city=Lisboa, brand=Brand 1
              Gasoleo A=1.5
              Gasolina 95 E5=1.7
            """.trimIndent()

        assertParsedStations(input, expected)
    }

    @Test
    fun `keeps stations with different ids separate`() {
        val input =
            portugalApiResponseJson(
                listOf(
                    stationJson(id = 1, product = "Gasóleo simples", price = "1,500", city = "Lisboa"),
                    stationJson(id = 2, product = "Gasolina 95 E5", price = "1,700", city = "Porto"),
                ),
            )

        val expected =
            """
            Stations: 2
            Station[1]: name=Station 1, city=Lisboa, brand=Brand 1
              Gasoleo A=1.5
            Station[2]: name=Station 2, city=Porto, brand=Brand 2
              Gasolina 95 E5=1.7
            """.trimIndent()

        assertParsedStations(input, expected)
    }

    @Test
    fun `passes through unknown products without mapping`() {
        val input = portugalApiResponseJson(listOf(stationJson(id = 1, product = "Unknown Fuel", price = "2,000")))

        val expected =
            """
            Stations: 1
            Station[1]: name=Station 1, city=Lisboa, brand=Brand 1
              Unknown Fuel=2.0
            """.trimIndent()

        assertParsedStations(input, expected)
    }

    @Test
    fun `converts GNC price from per cubic meter to per kilogram`() {
        // 340.000 €/m³ ÷ 170 kg/m³ = 2.0 €/kg
        val input =
            portugalApiResponseJson(
                listOf(stationJson(id = 1, product = "GNC (gás natural comprimido) - €/m3", price = "340,000")),
            )

        val expected =
            """
            Stations: 1
            Station[1]: name=Station 1, city=Lisboa, brand=Brand 1
              Gas Natural Comprimido=2.0
            """.trimIndent()

        assertParsedStations(input, expected)
    }

    @Test
    fun `passes through unsupported product gasolina de mistura`() {
        val input =
            portugalApiResponseJson(
                listOf(stationJson(id = 1, product = "Gasolina de mistura (motores a 2 tempos)", price = "2,500")),
            )

        val expected =
            """
            Stations: 1
            Station[1]: name=Station 1, city=Lisboa, brand=Brand 1
              Gasolina de mistura (motores a 2 tempos)=2.5
            """.trimIndent()

        assertParsedStations(input, expected)
    }

    @Test
    fun `passes through unsupported heating diesel`() {
        val input =
            portugalApiResponseJson(listOf(stationJson(id = 1, product = "Gasóleo de aquecimento", price = "0,900")))

        val expected =
            """
            Stations: 1
            Station[1]: name=Station 1, city=Lisboa, brand=Brand 1
              Gasóleo de aquecimento=0.9
            """.trimIndent()

        assertParsedStations(input, expected)
    }

    @Test
    fun `all mappable products are correctly translated to Spanish names`() {
        val input =
            portugalApiResponseJson(
                listOf(
                    // Diesel variants
                    stationJson(id = 1, product = "Gasóleo especial", price = "1,600"),
                    stationJson(id = 1, product = "Gasóleo simples", price = "1,500"),
                    stationJson(id = 1, product = "Gasóleo colorido", price = "1,400"),
                    stationJson(id = 1, product = "Biodiesel B15", price = "1,450"),
                    // Gasolina variants
                    stationJson(id = 1, product = "Gasolina especial 95", price = "1,750"),
                    stationJson(id = 1, product = "Gasolina simples 95", price = "1,650"),
                    stationJson(id = 1, product = "Gasolina 98", price = "1,850"),
                    // Gases (excluding GNC - tested separately)
                    stationJson(id = 1, product = "GPL Auto", price = "0,800"),
                    stationJson(id = 1, product = "GNL (gás natural liquefeito) - €/kg", price = "1,200"),
                ),
            )

        // Products sorted alphabetically by key, one per line
        val expected =
            """
            Stations: 1
            Station[1]: name=Station 1, city=Lisboa, brand=Brand 1
              Biodiesel=1.45
              Gas Natural Licuado=1.2
              Gases licuados del petróleo=0.8
              Gasoleo A=1.5
              Gasoleo B=1.4
              Gasoleo Premium=1.6
              Gasolina 95 E5=1.65
              Gasolina 95 E5 Premium=1.75
              Gasolina 98 E5=1.85
            """.trimIndent()

        assertParsedStations(input, expected)
    }

    @Test
    fun `all non-mappable products pass through with Portuguese names`() {
        val input =
            portugalApiResponseJson(
                listOf(
                    stationJson(id = 1, product = "Gasóleo de aquecimento", price = "0,900"),
                    stationJson(id = 1, product = "Gasolina especial 98", price = "1,900"),
                    stationJson(id = 1, product = "Gasolina de mistura (motores a 2 tempos)", price = "2,500"),
                ),
            )

        // Products sorted alphabetically by key, one per line
        val expected =
            """
            Stations: 1
            Station[1]: name=Station 1, city=Lisboa, brand=Brand 1
              Gasolina de mistura (motores a 2 tempos)=2.5
              Gasolina especial 98=1.9
              Gasóleo de aquecimento=0.9
            """.trimIndent()

        assertParsedStations(input, expected)
    }
}
