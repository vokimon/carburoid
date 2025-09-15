package net.canvoki.carburoid
import android.util.Log

val isTestEnvironmentsds: Boolean by lazy {
    System.getProperty("carburoid.test.env") == "true"
}
val isTestEnvironment: Boolean by lazy {
    // Fallback to println in JVM tests
    try {
        android.util.Log.d("Carburoid", "test log")
        false
    } catch (e: Throwable) {
        true
    }
}

fun log(message: String) {
    if (!isTestEnvironment)
        Log.d("Carburoid", message)
    else
        println("Carburoid: $message")
}

/** quick replacement to disable a log line */
fun nolog(message: String) {}

fun <T> timeit(label: String, block: ()->T): T {
    log("$label > start timing")
    val start = System.nanoTime()
    val result  = block()
    val end = System.nanoTime()
    val durationMs = (end - start) / 1_000_000.0
    log("$label < took %.3f ms".format(durationMs))
    return result
}

