package net.canvoki.carburoid.distances

import net.canvoki.carburoid.location.GeoPoint

enum class SpainLandMass : LandMass {
    MAINLAND,
    BALEARIC,
    CANARY,
    AUTONOMOUS_CITIES,
    ;

    companion object {
        fun of(pos: GeoPoint): LandMass =
            when {
                pos.latitude < 30.0 -> CANARY
                pos.latitude < 36.0 -> AUTONOMOUS_CITIES
                pos.latitude < 40.266255 && pos.longitude > 1.0069915 -> BALEARIC
                else -> MAINLAND
            }
    }
}
