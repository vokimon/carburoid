enum class LandMass {
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
                latitude < 30.0 -> LandMass.CANARY
                latitude < 36.0 -> LandMass.AUTONOMOUS_CITIES
                latitude < 40.266255 && longitude > 1.0069915 -> LandMass.BALEARIC
                else -> LandMass.MAINLAND
            }
    }
}
