package net.canvoki.carburoid.distances

import net.canvoki.carburoid.location.GeoPoint

enum class PortugalLandMass : LandMass {
    MAINLAND,
    ;

    companion object {
        fun of(pos: GeoPoint): LandMass = PortugalLandMass.MAINLAND
    }
}
