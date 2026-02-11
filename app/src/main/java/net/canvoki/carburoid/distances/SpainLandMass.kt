package net.canvoki.carburoid.distances

enum class SpainLandMass : LandMass {
    MAINLAND,
    BALEARIC,
    CANARY,
    AUTONOMOUS_CITIES,
    ;

    companion object {
        fun of(
            latitude: Double,
            longitude: Double,
        ): LandMass =
            when {
                latitude < 30.0 -> CANARY
                latitude < 36.0 -> AUTONOMOUS_CITIES
                latitude < 40.266255 && longitude > 1.0069915 -> BALEARIC
                else -> MAINLAND
            }
    }
}
