enum class LandMass {
    MAINLAND,
    BALEARIC,
    CANARY,
    AUTONOMOUS_CITIES,
    UNKNOWN,
    ALL,
    ;

    companion object {
        fun of(
            latitude: Double,
            longitude: Double,
        ): LandMass {
            fun isInRange(
                value: Double,
                min: Double,
                max: Double,
            ): Boolean = value >= min && value <= max

            fun isNear(
                lat: Double,
                lon: Double,
                tolerance: Double = 0.2,
            ): Boolean = kotlin.math.abs(latitude - lat) <= tolerance && kotlin.math.abs(longitude - lon) <= tolerance
            return when {
                isInRange(latitude, 38.6, 40.1) && isInRange(longitude, 1.1, 4.3) -> LandMass.BALEARIC
                isInRange(latitude, 36.0, 43.8) && isInRange(longitude, -9.3, 3.0) -> LandMass.MAINLAND
                isInRange(latitude, 27.5, 29.5) && isInRange(longitude, -18.0, -13.5) -> LandMass.CANARY
                isNear(35.9, -5.3) -> LandMass.AUTONOMOUS_CITIES // Ceuta
                isNear(35.3, -2.9) -> LandMass.AUTONOMOUS_CITIES // Melilla
                else -> LandMass.UNKNOWN
            }
        }
    }
}
