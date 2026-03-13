package net.canvoki.carburoid.algorithms

import net.canvoki.carburoid.distances.CurrentDistancePolicy
import net.canvoki.carburoid.model.GasStation
import java.time.Duration
import java.time.Instant

class StationFilter(
    val config: FilterConfig = FilterConfig(),
) {
    var stations = emptyList<GasStation>()
    var sortedStations = emptyList<GasStation>()
    var filteredStations = emptyList<GasStation>()

    fun filter(stations: List<GasStation>): List<GasStation> {
        this.stations = stations
        computeCrowDistances()
        sortByDistance()
        paretoFilter()
        return filteredStations
    }

    private fun computeCrowDistances() {
        for (station in stations) {
            station.computeDistance()
        }
    }

    private fun sortByDistance() {
        sortedStations = stations.sortedBy { it.distanceInMeters }
    }

    private fun paretoFilter() {
        var minPrice = 1000.0
        val result = mutableListOf<GasStation>()
        val deadLine = Instant.now().plus(Duration.ofMinutes(config.hideClosedMarginInMinutes.toLong()))
        for (station in sortedStations) {
            var mayCutoffPrice = true
            val stationPrice = station.price
            if (stationPrice == null) {
                //log("Filtered non number ${station.price}")
                continue
            }
            if (CurrentDistancePolicy.isBeyondSea(station)) {
                if (config.hideBeyondSea) {
                    continue
                }
            }
            if (!station.isPublicPrice) {
                if (config.onlyPublicPrices) {
                    //log("Filtered non public price")
                    continue
                }
                // Non-public prices may be shown but don't lower the bar
                mayCutoffPrice = false
            }

            if (config.hideExpensiveFurther && stationPrice > minPrice) {
                //log("Filtered $stationPrice vs $minPrice")
                continue
            }

            var status = station.openStatus(Instant.now())
            if (config.hideClosedMarginInMinutes < 7 * 24 * 60) {
                if (status.isOpen != true) {
                    if (status.until == null) {
                        //log("Filtered permanently closed station ${station.name} ${station.city}")
                        continue
                    } else if (status.until > deadLine) {
                        //log("Filtered currently closed station ${station.name} ${station.city}, opens at ${status.until}")
                        continue
                    }
                    // A closed station does not lower the bar
                    mayCutoffPrice = false
                }
            }

            if (mayCutoffPrice) {
                //log("Updating price to ${stationPrice} ${station.isPublicPrice}")
                minPrice = stationPrice
            }
            result.add(station)
        }
        filteredStations = result
    }
}
