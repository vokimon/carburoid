package net.canvoki.carburoid.plotnavigator

fun <T, X : Number, Y : Number> prepareHistogram2D(
    samples: List<T>,
    nBinsX: Int,
    nBinsY: Int,
    xGetter: (T) -> X,
    yGetter: (T) -> Y,
    xMin: X,
    xMax: X,
    yMin: Y,
    yMax: Y,
): BinGrid<Int> {
    require(nBinsX > 0 && nBinsY > 0) { "Number of bins must be positive." }

    val xRange = xMax.toFloat() - xMin.toFloat()
    val yRange = yMax.toFloat() - yMin.toFloat()

    val bins = Array(nBinsX) { Array<Int>(nBinsY) { 0 } }

    for (sample in samples) {
        val x = xGetter(sample).toFloat()
        val y = yGetter(sample).toFloat()

        val bx = (((x - xMin.toFloat()) / xRange) * nBinsX).toInt()
        val by = (((y - yMin.toFloat()) / yRange) * nBinsY).toInt()

        if (bx !in 0 until nBinsX) continue
        if (by !in 0 until nBinsY) continue

        bins[bx][by]++
    }

    return bins
}
