package net.canvoki.carburoid.model

import net.canvoki.carburoid.distances.CurrentDistancePolicy
import net.canvoki.carburoid.product.ProductManager
import java.time.Instant
import java.time.ZoneId

interface GasStation {
    val id: Int
    val name: String?
    val address: String?
    val city: String?
    val state: String?
    val latitude: Double?
    val longitude: Double?
    val isPublicPrice: Boolean
    val openingHours: OpeningHours?
    val prices: Map<String, Double?>

    val distanceInMeters: Float?

    val price: Double?
        get() = prices[ProductManager.getCurrent()]

    fun computeDistance()

    fun openStatus(instant: Instant) =
        openingHours?.getStatus(instant, timeZone())
            ?: OpeningStatus(isOpen = false, until = null)

    fun timeZone(): ZoneId =
        if ((longitude ?: 0.0) > -10.0) {
            ZoneId.of("Europe/Madrid")
        } else {
            ZoneId.of("Atlantic/Canary")
        }

    fun toJson(): String

    companion object {
        fun parse(json: String): GasStation = SpanishGasStation.parse(json)
    }
}

abstract class BaseGasStation : GasStation {
    private var _distanceInMeters: Float? = null
    override val distanceInMeters: Float?
        get() = _distanceInMeters

    override fun computeDistance() {
        _distanceInMeters = CurrentDistancePolicy.getDistance(this)
    }
}

interface GasStationResponse {
    val stations: List<GasStation>
}
