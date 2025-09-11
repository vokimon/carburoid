package net.canvoki.carburoid.model

import com.google.gson.annotations.SerializedName

data class GasStationResponse(
    @SerializedName("ListaEESSPrecio")
    val stations: List<GasStation>
)

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
    val priceGasoleoA: String?  // ← Renamed to valid identifier
)
