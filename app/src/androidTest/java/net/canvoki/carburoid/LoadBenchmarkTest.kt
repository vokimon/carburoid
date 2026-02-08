package net.canvoki.carburoid

import androidx.test.platform.app.InstrumentationRegistry
import net.canvoki.carburoid.model.GasStationResponseGson
import net.canvoki.carburoid.model.SpanishGasStationResponse
import org.junit.Test
import java.io.File
import java.time.Instant

open class LoadBenchmarkGsonTest {
    open fun parse(jsonContent: String): Int {
        val response = GasStationResponseGson.parse(jsonContent)
        return response.stations.size
    }

    @Test
    fun testParsingPerformance() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputStream = context.resources.openRawResource(R.raw.sample_gas_stations)
        val jsonContent = inputStream.bufferedReader().use { it.readText() }

        val iterations = 10
        val times = mutableListOf<Long>()
        var size = 0

        repeat(iterations) {
            val startTime = System.currentTimeMillis()
            size = parse(jsonContent)
            val endTime = System.currentTimeMillis()
            times.add(endTime - startTime)
            log("Iteration $it: ${endTime - startTime}ms, stations: $size")
        }

        val avgTime = times.average()
        val minTime = times.minOrNull()
        val maxTime = times.maxOrNull()

        log("Parsing stats: $this")
        log("  Average: ${avgTime}ms")
        log("  Min: ${minTime}ms")
        log("  Max: ${maxTime}ms")
        log("  Stations: $size")

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
        log("BENCHMARK_RESULTS: " + resultLine)
    }
}

class LoadBenchmarkKSerialTest : LoadBenchmarkGsonTest() {
    override fun parse(jsonContent: String): Int {
        val response = SpanishGasStationResponse.parse(jsonContent)
        return response.stations.size
    }
}
