package net.canvoki.carburoid.algorithms

import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.distances.CurrentDistancePolicy

class StationFilter {
    fun filterParetoOptimal(stations: List<GasStation>): List<GasStation> {
        for (station in stations) {
            station.computeDistance()
            println(station.distanceInMeters)
        }

        return stations
            .sortedBy { it.distanceInMeters }
    }
}
