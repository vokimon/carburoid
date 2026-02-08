package net.canvoki.carburoid.model

import net.canvoki.carburoid.product.ProductManager
import net.canvoki.carburoid.test.assertJsonEqual
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

@Suppress("ktlint:standard:max-line-length")
val fullJsonCase = """{"IDEESS":1234,"Rótulo":"Test Station","Dirección":"Calle Principal 123","Localidad":"Madrid","Provincia":"Madrid","Latitud":"40,4168","Longitud (WGS84)":"-3,7038","Tipo Venta":"P","Horario":"L-D: 24H","Precio Gasolina 95":"1,234"}"""


abstract class GasStationFixture {
    private lateinit var originalLocale: Locale

    @Before
    fun saveLocale() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.ROOT)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    abstract fun parseResponse(string: String): GasStation

    abstract fun parseStation(string: String): GasStation

    abstract fun newGasStation(
        id: Int,
        name: String?,
        address: String?,
        city: String?,
        state: String?,
        latitude: Double?,
        longitude: Double?,
        isPublicPrice: Boolean,
        prices: Map<String, Double?>,
    ): GasStation

    @Test
    fun `parse station with valid coordinates`() {
        // Sample JSON for one station
        val json =
            """
            {
                "IDEESS": "666",
                "Rótulo": "REPSOL",
                "Dirección": "Calle Mayor 1",
                "Localidad": "Madrid",
                "Provincia": "Madrid",
                "Precio Gasoleo A": "1,659",
                "Latitud": "40,4168",
                "Longitud (WGS84)": "-3,7038",
                "Horario": "L-D: 10:00-20:00",
                "Tipo Venta": "P"
            }
            """.trimIndent()

        val station = parseStation(json)

        // Verify computed properties
        assertEquals("REPSOL", station.name)
        assertEquals("Madrid", station.city)
        assertEquals(40.4168, station.latitude!!, 0.0001)
        assertEquals(-3.7038, station.longitude!!, 0.0001)
        assertEquals(1.659, station.price!!, 0.0001)
        assertEquals(true, station.isPublicPrice)
        assertEquals("L-D: 10:00-20:00", station.openingHours.toString())
    }

    @Test
    fun `parse station with blank coordinates`() {
        val json =
            """
            {
                "IDEESS": "666",
                "Rótulo": "CEPSA",
                "Dirección": "Gran Vía 2",
                "Localidad": "Madrid",
                "Provincia": "Madrid",
                "Precio Gasoleo A": "1,670",
                "Latitud": "",
                "Longitud (WGS84)": "   ",
                "Tipo Venta": "P"
            }
            """.trimIndent()

        val station = parseStation(json)

        // Should be null for blank/whitespace
        assertEquals(null, station.latitude)
        assertEquals(null, station.longitude)
    }

    @Test
    fun `parse station prices available as map`() {
        val json =
            """
            {
                "IDEESS": "666",
                "Rótulo": "CEPSA",
                "Dirección": "Gran Vía 2",
                "Localidad": "Madrid",
                "Provincia": "Madrid",
                "Precio Gasoleo A": "1,670",
                "Latitud": "",
                "Longitud (WGS84)": "   ",
                "Tipo Venta": "P"
            }
            """.trimIndent()

        val station = parseStation(json)

        assertEquals(mapOf("Gasoleo A" to 1.670), station.prices)
    }

    @Test
    fun `parse station unexpected prices`() {
        val json =
            """
            {
                "IDEESS": "666",
                "Rótulo": "CEPSA",
                "Dirección": "Gran Vía 2",
                "Localidad": "Madrid",
                "Provincia": "Madrid",
                "Precio Gasoleo A": "1,670",
                "Precio My product": "2,000",
                "Latitud": "",
                "Longitud (WGS84)": "   "
            }
            """.trimIndent()

        val station = parseStation(json)

        assertEquals(
            mapOf(
                "Gasoleo A" to 1.670,
                "My product" to 2.000,
            ),
            station.prices,
        )
    }

    fun twoProductsStation(): GasStation =
        parseStation(
            """
            {
                "IDEESS": "666",
                "Rótulo": "CEPSA",
                "Dirección": "Gran Vía 2",
                "Localidad": "Madrid",
                "Provincia": "Madrid",
                "Precio Gasoleo A": "1,670",
                "Precio My product": "2,000",
                "Latitud": "",
                "Longitud (WGS84)": "   ",
                "Tipo Venta": "P"
            }""",
        )

    @After
    fun resetCurrentProduct() {
        ProductManager.resetCurrent()
    }

    @Test
    fun `price changes product`() {
        val station = twoProductsStation()
        val gasoleoAPrice = station.prices["Gasoleo A"]
        val myProductPrice = station.prices["My product"]

        assertEquals(gasoleoAPrice, station.price) // Default Gasoleo A

        ProductManager.setCurrent("My product")

        assertEquals(myProductPrice, station.price) // Set My Product

        ProductManager.setCurrent("Missing product")

        assertEquals(null, station.price) // Missing product in this GasStation

        ProductManager.resetCurrent()

        assertEquals(gasoleoAPrice, station.price) // Back to default Gasoleo A
    }

    @Test
    fun `parse station list`() {
        val json =
            """
            {
                "ListaEESSPrecio": [
                    {
                        "IDEESS": "666",
                        "Rótulo": "REPSOL",
                        "Dirección": "Calle Mayor 1",
                        "Localidad": "Madrid",
                        "Provincia": "Madrid",
                        "Precio Gasoleo A": "1,659",
                        "Latitud": "40,4168",
                        "Longitud (WGS84)": "-3,7038",
                        "Tipo Venta": "P"
                    }
                ]
            }
            """.trimIndent()

        val station = parseResponse(json)

        assertEquals(40.4168, station.latitude!!, 0.0001)
        assertEquals(-3.7038, station.longitude!!, 0.0001)
    }

    fun `parse station with no Horarios gets null`() {
        val json =
            """
            {
                "IDEESS": "666",
                "Rótulo": "REPSOL",
                "Dirección": "Calle Mayor 1",
                "Localidad": "Madrid",
                "Provincia": "Madrid",
                "Precio Gasoleo A": "1.659",
                "Latitud": "40,4168",
                "Longitud (WGS84)": "-3,7038",
                "Tipo Venta": "P"
            }
            """.trimIndent()

        val station = parseStation(json)

        // Verify computed properties
        assertEquals(null, station.openingHours)
    }

    @Test
    fun `toJson with data`() {
        val gasStation =
            newGasStation(
                id = 1234,
                name = "Test Station",
                address = "Calle Principal 123",
                city = "Madrid",
                state = "Madrid",
                latitude = 40.4168,
                longitude = -3.7038,
                isPublicPrice = true,
                prices = mapOf("Gasolina 95" to 1.234),
            )

        val json = gasStation.toJson()

        //println("JSON serialitzat: $json")
        assertJsonEqual(
            fullJsonCase,
            json,
        )
    }

    @Test
    fun `toJson with nulls`() {
        val gasStation =
            newGasStation(
                id = 1234, // Not null
                name = null,
                address = null,
                city = null,
                state = null,
                latitude = null,
                longitude = null,
                isPublicPrice = false, // Not null
                prices = mapOf("Gasolina 95" to null),
            )

        val json = gasStation.toJson()

        //println("JSON serialitzat: $json")
        assertJsonEqual("""{"IDEESS":1234,"Tipo Venta":"R","Horario":"L-D: 24H"}""", json)
    }

    @Test
    fun `toJson with exotic locale, arab`() {
        Locale.setDefault(Locale.forLanguageTag("ar")) // Arabic serializes its own numbers
        val gasStation =
            newGasStation(
                id = 1234,
                name = "Test Station",
                address = "Calle Principal 123",
                city = "Madrid",
                state = "Madrid",
                latitude = 40.4168,
                longitude = -3.7038,
                isPublicPrice = true,
                prices = mapOf("Gasolina 95" to 1.234),
            )

        val json = gasStation.toJson()

        assertJsonEqual(fullJsonCase, json)
    }

    @Test
    fun testRoundTrip() {
        val originalStation =
            newGasStation(
                id = 1234,
                name = "Test Station",
                address = "Calle Principal 123",
                city = "Madrid",
                state = "Madrid",
                latitude = 40.4168,
                longitude = -3.7038,
                isPublicPrice = true,
                prices = mapOf("Gasolina 95" to 1.234),
            )

        val json = originalStation.toJson()
        //println("JSON serialitzat: $json")
        val deserializedStation = parseStation(json)

        assertEquals(originalStation.id, deserializedStation.id)
        assertEquals(originalStation.latitude, deserializedStation.latitude)
        assertEquals(originalStation.longitude, deserializedStation.longitude)
        assertEquals(originalStation.prices, deserializedStation.prices)
    }
}

class GsonGasStationTest : GasStationFixture() {
    override fun parseResponse(string: String): GasStation {
        val response = GasStationResponseGson.parse(string)
        return response.stations.first()
    }

    override fun parseStation(string: String): GasStation = GasStationGson.parse(string)

    open override fun newGasStation(
        id: Int,
        name: String?,
        address: String?,
        city: String?,
        state: String?,
        latitude: Double?,
        longitude: Double?,
        isPublicPrice: Boolean,
        prices: Map<String, Double?>,
    ): GasStation =
        GasStationGson(
            id = id,
            name = name,
            address = address,
            city = city,
            state = state,
            latitude = latitude,
            longitude = longitude,
            isPublicPrice = isPublicPrice,
            prices = prices,
        )
}

class SpanishGasStationTest : GasStationFixture() {
    override fun parseResponse(string: String): GasStation {
        val response = SpanishGasStationResponse.parse(string)
        return response.stations.first()
    }

    override fun parseStation(string: String): GasStation = SpanishGasStation.parse(string)

    override fun newGasStation(
        id: Int,
        name: String?,
        address: String?,
        city: String?,
        state: String?,
        latitude: Double?,
        longitude: Double?,
        isPublicPrice: Boolean,
        prices: Map<String, Double?>,
    ): GasStation =
        SpanishGasStation(
            id = id,
            name = name,
            address = address,
            city = city,
            state = state,
            latitude = latitude,
            longitude = longitude,
            isPublicPrice = isPublicPrice,
            prices = prices,
        )
}
