package net.canvoki.carburoid.distances

interface LandMass

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

enum class FranceLandMass : LandMass {
    MAINLAND,
    CORSICA,
    ;

    companion object {
        fun of(
            latitude: Double,
            longitude: Double,
        ): LandMass =
            when {
                latitude >= 41.3 && latitude <= 43.1 && longitude >= 8.5 && longitude <= 9.5 -> FranceLandMass.CORSICA
                else -> FranceLandMass.MAINLAND
            }
    }
}
