package net.canvoki.carburoid.algorithms

import java.time.Instant
import java.time.Duration
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.distances.CurrentDistancePolicy
import net.canvoki.carburoid.log

class StationFilter (
    var config : FilterConfig = FilterConfig()
) {
    fun filter(stations: List<GasStation>): List<GasStation> {
        for (station in stations) {
            station.computeDistance()
        }

        val sortedStations = stations
            .sortedBy { it.distanceInMeters }
        var minPrice = 1000.0
        val result = mutableListOf<GasStation>()
        val deadLine = Instant.now().plus(Duration.ofMinutes(config.hideClosedMarginInMinutes.toLong()))
        for (station in sortedStations) {
            var canLimit = true
            val stationPrice = station.price
            if (stationPrice == null) {
                //log("Filtered non number ${station.price}")
                continue
            }
            if (!station.isPublicPrice) {
                if (config.onlyPublicPrices) {
                    //log("Filtered non public price")
                    continue
                }
                canLimit = false
            }
            if (config.hideExpensiveFurther && stationPrice > minPrice) {
                //log("Filtered $stationPrice vs $minPrice")
                continue
            }

            var status = station.openStatus(Instant.now())
            if (status?.isOpen != true) {
                if (status.until == null) {
                    //log("Filtered permanently closed station ${station.name} ${station.city}")
                    continue
                }
                if (status.until > deadLine) {
                    //log("Filtered currently closed station ${station.name} ${station.city}")
                    continue
                }
            }

            if (canLimit) {
                //log("Updating price to ${stationPrice} ${station.isPublicPrice}")
                minPrice = stationPrice
            }
            result.add(station)
        }
        return result
    }
}
