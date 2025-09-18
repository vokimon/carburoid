package net.canvoki.carburoid.algorithms

import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.distances.CurrentDistancePolicy

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
        for (station in sortedStations) {
            val stationPrice = station.price
            if (stationPrice == null) {
                //log("Filtered non number ${station.price}")
                continue
            }
            if (config.hideExpensiveFurther && stationPrice > minPrice) {
                //log("Filtered $stationPrice vs $minPrice")
                continue
            }
            minPrice = stationPrice
            result.add(station)
        }
        return result
    }
}
