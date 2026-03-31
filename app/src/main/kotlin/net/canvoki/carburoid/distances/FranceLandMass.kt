package net.canvoki.carburoid.distances

import net.canvoki.carburoid.location.GeoPoint

enum class FranceLandMass : LandMass {
    MAINLAND,
    CORSICA,
    ;

    companion object {
        fun of(pos: GeoPoint): LandMass =
            when {
                pos.latitude >= 41.3 &&
                    pos.latitude <= 43.1 &&
                    pos.longitude >= 8.5 &&
                    pos.longitude <= 9.5 ->
                    FranceLandMass.CORSICA
                else -> FranceLandMass.MAINLAND
            }
    }
}
