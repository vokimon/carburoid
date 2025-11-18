package net.canvoki.carburoid.json

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals

class SpanishDateTest {
    fun toCase(
        expected: String?,
        input: String?,
    ) {
        val instant = input?.let { Instant.parse(it) }
        assertEquals(expected, toSpanishDate(instant))
    }

    @Test fun `toSpanishDate null`() = toCase(null, null)

    @Test fun `toSpanishDate valid`() = toCase("01/06/2024 9:30:45", "2024-06-01T07:30:45Z")

    @Test fun `toSpanishDate midnight`() = toCase("01/06/2024 2:00:00", "2024-06-01T00:00:00Z")

    @Test fun `toSpanishDate single-digit hour`() = toCase("01/06/2024 3:05:00", "2024-06-01T01:05:00Z")

    @Test fun `toSpanishDate double-digit hour`() = toCase("01/06/2024 22:59:59", "2024-06-01T20:59:59Z")

    @Test fun `toSpanishDate winter time +1`() = toCase("01/11/2024 1:00:00", "2024-11-01T00:00:00Z")

    @Test fun `toSpanishDate first dupped daylight date`() = toCase("27/10/2024 2:30:00", "2024-10-27T00:30:00Z")

    @Test fun `toSpanishDate second dupped daylight date`() = toCase("27/10/2024 2:30:00", "2024-10-27T01:30:00Z")

    fun fromCase(
        expected: String?,
        input: String?,
    ) {
        val parsedExpected = expected?.let { Instant.parse(it) }
        assertEquals(parsedExpected, fromSpanishDate(input))
    }

    @Test fun `fromSpanishDate null`() = fromCase(null, null)

    @Test fun `fromSpanishDate empty string`() = fromCase(null, "")

    @Test fun `fromSpanishDate invalid format`() = fromCase(null, "not a date")

    @Test fun `fromSpanishDate valid`() = fromCase("2024-06-01T07:30:45Z", "01/06/2024 9:30:45")

    @Test fun `fromSpanishDate midnight`() = fromCase("2024-05-31T22:00:00Z", "01/06/2024 0:00:00")

    @Test fun `fromSpanishDate just before spring +1`() = fromCase("2024-03-31T00:59:59Z", "31/03/2024 1:59:59")

    @Test fun `fromSpanishDate just after spring +2`() = fromCase("2024-03-31T01:00:00Z", "31/03/2024 3:00:00")

    @Test fun `fromSpanishDate dupped daylight date, arbitrarily takes the earlier one`() =
        fromCase(
            "2024-10-27T00:30:00Z",
            "27/10/2024 2:30:00",
        )

    @Test fun `fromSpanishDate mising daylight date, uses winter time`() = fromCase("2024-03-31T01:15:00Z", "31/03/2024 2:15:00")

    private val adapter = SpanishDateTypeAdapter()

    @Test
    fun `type adapter reads invalid date as null`() {
        val reader = JsonReader(java.io.StringReader("\"not a date\""))

        val result = adapter.read(reader)

        assertEquals(null, result)
    }

    @Test
    fun `type adapter reads valid date`() {
        val reader = JsonReader(java.io.StringReader("\"01/06/2024 9:30:45\""))
        val expected = Instant.parse("2024-06-01T07:30:45Z")

        val result = adapter.read(reader)

        assertEquals(expected, result)
    }

    @Test
    fun `type adapter writes null`() {
        val writer = java.io.StringWriter()
        val jsonWriter = JsonWriter(writer)

        adapter.write(jsonWriter, null)
        jsonWriter.flush()

        assertEquals("null", writer.toString())
    }

    @Test
    fun `type adapter writes valid date`() {
        val writer = java.io.StringWriter()
        val jsonWriter = JsonWriter(writer)
        val input = Instant.parse("2024-06-01T07:30:45Z") // UTC

        adapter.write(jsonWriter, input)
        jsonWriter.flush()

        assertEquals("\"01/06/2024 9:30:45\"", writer.toString())
    }
}
