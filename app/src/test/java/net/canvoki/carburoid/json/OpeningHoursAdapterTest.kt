package net.canvoki.carburoid.json

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.canvoki.carburoid.model.OpeningHours
import org.junit.Assert.assertEquals
import org.junit.Test

class OpeningHoursAdapterTest {

    val adapter = OpeningHoursAdapter()

    @Test
    fun `type adapter reads null as null`() {
        val reader = JsonReader(java.io.StringReader("null"))

        val result = adapter.read(reader)

        assertEquals(null, result)
    }

    @Test
    fun `type adapter reads empty as null`() {
        val reader = JsonReader(java.io.StringReader("\"\""))

        val result = adapter.read(reader)

        assertEquals(null, result)
    }

    @Test
    fun `type adapter reads opening hours`() {
        val reader = JsonReader(java.io.StringReader("\"V: 24H\""))

        val result = adapter.read(reader)

        assertEquals("V: 24H", result.toString())
    }

    @Test
    fun `type adapter writes empty string when empty object`() {
        val writer = java.io.StringWriter()
        val jsonWriter = JsonWriter(writer)

        adapter.write(jsonWriter, OpeningHours())
        jsonWriter.flush()

        assertEquals("\"\"", writer.toString())
    }

    @Test
    fun `type adapter writes empty string when null`() {
        val writer = java.io.StringWriter()
        val jsonWriter = JsonWriter(writer)

        adapter.write(jsonWriter, null)
        jsonWriter.flush()

        assertEquals("\"\"", writer.toString())
    }

    @Test
    fun `type adapter writes encoded when filled object`() {
        val writer = java.io.StringWriter()
        val jsonWriter = JsonWriter(writer)

        adapter.write(jsonWriter, OpeningHours.parse("V: 24H"))
        jsonWriter.flush()

        assertEquals("\"V: 24H\"", writer.toString())
    }
}
