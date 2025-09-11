package net.canvoki.carburoid.model

import android.util.Log
import com.google.gson.annotations.SerializedName


data class GasStationResponse(
    @SerializedName("ListaEESSPrecio")
    val stations: List<GasStation>
)

fun log(message: String) {
    println("Carburoid: $message")
}

data class GasStation(
    @SerializedName("Rótulo")
    val name: String?,

    @SerializedName("Dirección")
    val address: String?,

    @SerializedName("Localidad")
    val city: String?,

    @SerializedName("Provincia")
    val state: String?,

    @SerializedName("Precio Gasoleo A")
    val priceGasoleoA: String?,

    @SerializedName("Latitud")
    val latStr: String?,

    @SerializedName("Longitud (WGS84)")
    val lngStr: String?,

) {
    init {
        log("init: name=$name, latStr=$latStr, lngStr=$lngStr")
    }
    val latitude: Double?
        get() = latStr
            ?.takeIf { it.isNotBlank() }
            ?.replace(',', '.')
            ?.toDoubleOrNull()
            //?.also { log("Parsed latitude: $it from '$latStr'") }

    val longitude: Double?
        get() = lngStr
            ?.takeIf { it.isNotBlank() }
            ?.replace(',', '.')
            ?.toDoubleOrNull()
            //?.also { log("Parsed longitude: $it from '$lngStr'") }
}
