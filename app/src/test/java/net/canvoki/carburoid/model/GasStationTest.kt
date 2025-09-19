package net.canvoki.carburoid.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.After

class GasStationTest {

    @Test
    fun `parse station with valid coordinates`() {
        // Sample JSON for one station
        val json = """
            {
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

        // Parse with Gson
        val station = GasStation.parse(json)

        // Verify computed properties
        assertEquals("REPSOL", station.name)
        assertEquals("Madrid", station.city)
        assertEquals(40.4168, station.latitude!!, 0.0001)
        assertEquals(-3.7038, station.longitude!!, 0.0001)
        assertEquals(1.659, station.price!!, 0.0001)
        assertEquals(true, station.isPublicSale)
    }

    @Test
    fun `parse station with blank coordinates`() {
        val json = """
            {
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

        val station = GasStation.parse(json)

        // Should be null for blank/whitespace
        assertEquals(null, station.latitude)
        assertEquals(null, station.longitude)
    }

    @Test
    fun `parse station prices available as map`() {
        val json = """
            {
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

        val station = GasStation.parse(json)

        assertEquals(mapOf("Gasoleo A" to 1.670), station.prices)
    }

    @Test
    fun `parse station unexpected prices`() {
        val json = """
            {
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

        val station = GasStation.parse(json)

        assertEquals(mapOf(
            "Gasoleo A" to 1.670,
            "My product" to 2.000,
        ), station.prices)
    }

    fun twoProductsStation() : GasStation {
        return GasStation.parse("""
            {
                "Rótulo": "CEPSA",
                "Dirección": "Gran Vía 2",
                "Localidad": "Madrid",
                "Provincia": "Madrid",
                "Precio Gasoleo A": "1,670",
                "Precio My product": "2,000",
                "Latitud": "",
                "Longitud (WGS84)": "   ",
                "Tipo Venta": "P"
            }""")
    }

    @After
    fun resetCurrentProduct() {
        GasStation.resetCurrentProduct()
    }

    @Test
    fun `price changes product`() {
        val station = twoProductsStation()
        val gasoleoAPrice = station.prices["Gasoleo A"]
        val myProductPrice = station.prices["My product"]

        assertEquals(gasoleoAPrice, station.price) // Default Gasoleo A

        GasStation.setCurrentProduct("My product")

        assertEquals(myProductPrice, station.price) // Set My Product

        GasStation.setCurrentProduct("Missing product")

        assertEquals(null, station.price) // Missing product in this GasStation

        GasStation.resetCurrentProduct()

        assertEquals(gasoleoAPrice, station.price) // Back to default Gasoleo A
    }



    @Test
    fun `parse station list`() {
        val json = """
            {
                "ListaEESSPrecio": [
                    {
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

        val response = GasStationResponse.parse(json)
        val station = response.stations.first()

        assertEquals(40.4168, station.latitude!!, 0.0001)
        assertEquals(-3.7038, station.longitude!!, 0.0001)
    }

}
