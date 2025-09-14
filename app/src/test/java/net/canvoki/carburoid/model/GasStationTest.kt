package net.canvoki.carburoid.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertEquals
import org.junit.Test

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
                "Longitud (WGS84)": "-3,7038"
            }
        """.trimIndent()

        // Parse with Gson
        val station = Gson().fromJson(json, GasStation::class.java)

        // Verify computed properties
        assertEquals("REPSOL", station.name)
        assertEquals("Madrid", station.city)
        assertEquals(40.4168, station.latitude!!, 0.0001)
        assertEquals(-3.7038, station.longitude!!, 0.0001)
        assertEquals(1.659, station.priceGasoleoA!!, 0.0001)
    }

    @Test
    fun `parse station with blank coordinates`() {
        val json = """
            {
                "Rótulo": "CEPSA",
                "Dirección": "Gran Vía 2",
                "Localidad": "Madrid",
                "Provincia": "Madrid",
                "Precio Gasoleo A": "1.670",
                "Latitud": "",
                "Longitud (WGS84)": "   "
            }
        """.trimIndent()

        val station = Gson().fromJson(json, GasStation::class.java)

        // Should be null for blank/whitespace
        assertEquals(null, station.latitude)
        assertEquals(null, station.longitude)
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
                        "Precio Gasoleo A": "1.659",
                        "Latitud": "40,4168",
                        "Longitud (WGS84)": "-3,7038"
                    }
                ]
            }
        """.trimIndent()

        val response = Gson().fromJson(json, GasStationResponse::class.java)
        val station = response.stations.first()

        assertEquals(40.4168, station.latitude!!, 0.0001)
        assertEquals(-3.7038, station.longitude!!, 0.0001)
    }
}
