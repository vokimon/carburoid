package net.canvoki.carburoid.algorithms

data class FilterConfig(
    val hideExpensiveFurther: Boolean = true,
    val onlyPublicPrices: Boolean = true,
    val hideClosedMarginInMinutes: Int = 120,
)
