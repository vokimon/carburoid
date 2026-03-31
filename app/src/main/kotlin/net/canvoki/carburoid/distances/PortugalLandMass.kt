package net.canvoki.carburoid.distances

enum class PortugalLandMass : LandMass {
    MAINLAND,
    ;

    companion object {
        fun of(
            latitude: Double,
            longitude: Double,
        ): LandMass = PortugalLandMass.MAINLAND
    }
}
