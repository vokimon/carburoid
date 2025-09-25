package net.canvoki.carburoid.algorithms

data class FilterConfig(
    val hideExpensiveFurther : Boolean = true,
    val onlyPublicPrices: Boolean = false,
    val hideClosedMarginInMinutes: Int = 30,
)
