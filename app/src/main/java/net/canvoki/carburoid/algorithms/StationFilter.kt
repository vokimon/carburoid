package net.canvoki.carburoid.algorithms

import android.util.Log
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.distances.CurrentDistancePolicy

class StationFilter {
    fun filterParetoOptimal(stations: List<GasStation>): List<GasStation> {
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
                //println("Filtered non number ${station.price}")
                continue
            }
            if (stationPrice > minPrice) {
                //println("Filtered $stationPrice vs $minPrice")
                continue
            }
            minPrice = stationPrice
            result.add(station)
        }
        return result
    }
}
