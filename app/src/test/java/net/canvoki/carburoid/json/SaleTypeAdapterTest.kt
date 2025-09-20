package net.canvoki.carburoid.json

import com.google.gson.stream.JsonWriter
import com.google.gson.stream.JsonReader
import org.junit.Assert.assertEquals
import org.junit.Test

class SaleTypeAdapterTest {

    val adapter = SaleTypeAdapter()

    @Test
    fun `type adapter reads R restricted as false`() {
        val reader = JsonReader(java.io.StringReader("\"R\""))

        val result = adapter.read(reader)

        assertEquals(false, result)
    }

    @Test
    fun `type adapter reads P public as grue`() {
        val reader = JsonReader(java.io.StringReader("\"P\""))

        val result = adapter.read(reader)

        assertEquals(true, result)
    }

    @Test
    fun `type adapter reads invalid as null`() {
        val reader = JsonReader(java.io.StringReader("\"BAD\""))

        val result = adapter.read(reader)

        assertEquals(null, result)
    }

    @Test
    fun `type adapter reads null as null`() {
        val reader = JsonReader(java.io.StringReader("null"))

        val result = adapter.read(reader)

        assertEquals(null, result)
    }

    @Test
    fun `type adapter writes empty string when null`() {
        val writer = java.io.StringWriter()
        val jsonWriter = JsonWriter(writer)

        adapter.write(jsonWriter, null)
        jsonWriter.flush()

        assertEquals(   "\"\"", writer.toString())
    }

    @Test
    fun `type adapter writes R when false`() {
        val writer = java.io.StringWriter()
        val jsonWriter = JsonWriter(writer)

        adapter.write(jsonWriter, false)
        jsonWriter.flush()

        assertEquals("\"R\"", writer.toString())
    }

    @Test
    fun `type adapter writes P when true`() {
        val writer = java.io.StringWriter()
        val jsonWriter = JsonWriter(writer)

        adapter.write(jsonWriter, true)
        jsonWriter.flush()

        assertEquals("\"P\"", writer.toString())
    }
}
