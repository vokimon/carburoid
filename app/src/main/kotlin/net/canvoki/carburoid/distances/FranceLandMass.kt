package net.canvoki.carburoid.distances

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
