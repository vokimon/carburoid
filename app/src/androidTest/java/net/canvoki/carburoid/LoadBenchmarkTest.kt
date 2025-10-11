package net.canvoki.carburoid

import net.canvoki.carburoid.model.GasStationResponse
import org.junit.Test
import java.io.File
import java.time.Instant
import net.canvoki.carburoid.log as println

class LoadBenchmarkTest {

    @Test
    fun testParsingPerformance() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val inputStream = context.resources.openRawResource(R.raw.sample_gas_stations)
        val jsonContent = inputStream.bufferedReader().use { it.readText() }

        val iterations = 1
        val times = mutableListOf<Long>()
        var size = 0

        repeat(iterations) {
            val startTime = System.currentTimeMillis()
            val response = GasStationResponse.parse(jsonContent)
            val endTime = System.currentTimeMillis()
            size = response?.stations?.size ?: 0
            times.add(endTime - startTime)
            println("Iteration $it: ${endTime - startTime}ms, stations: $size")
        }

        val avgTime = times.average()
        val minTime = times.minOrNull()
        val maxTime = times.maxOrNull()

        println("Parsing stats:")
        println("  Average: ${avgTime}ms")
        println("  Min: ${minTime}ms")
        println("  Max: ${maxTime}ms")
        println("  Stations: $size")

        // Escriu els resultats a un fitxer

        //val resultsFile = File(System.getProperty("user.dir"), "parsing_benchmark.tsv")
        val resultsFile = File(context.getExternalFilesDir(null), "parsing_benchmark.tsv")
        resultsFile.parentFile?.mkdirs()

        val isoInstant = Instant.now().toString()
        val resultLine = "$isoInstant\t${avgTime}\t${minTime}\t${maxTime}\t${size}\n"

        if (!resultsFile.exists()) {
            resultsFile.writeText("timestamp\tavg_ms\tmin_ms\tmax_ms\tstation_count\n")
        }

        resultsFile.appendText(resultLine)

        // Mostra el contingut acumulat del fitxer
        println("BENCHMARK_RESULTS: " + resultLine)
    }
}
