package net.canvoki.carburoid.json

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test

class SpanishFloatTest {
    @Test
    fun `preprocessSpanishNumbers`() {
        assertEquals("\"23.323\"", preprocessSpanishNumbers("\"23,323\""))
    }

    @Test
    fun `preprocessSpanishNumbers with negative numbers`() {
        assertEquals("\"-23.323\"", preprocessSpanishNumbers("\"-23,323\""))
    }

    @Test
    fun `postprocessSpanishNumbers`() {
        assertEquals("\"23,323\"", postprocessSpanishNumbers("\"23.323\""))
    }

    @Test
    fun `postprocessSpanishNumbers with negative numbers`() {
        assertEquals("\"-23,323\"", postprocessSpanishNumbers("\"-23.323\""))
    }

    @Test
    fun `toSpanishFloat`() {
        assertEquals("2,3", toSpanishFloat(2.3))
        assertEquals(null, toSpanishFloat(null))
        assertEquals("NaN", toSpanishFloat(Double.NaN))
    }

    @Test
    fun `fromSpanishFloat`() {
        assertEquals(2.3, fromSpanishFloat("2,3"))
        assertEquals(2.3, fromSpanishFloat("2.3")) // english still parsed properly
        assertEquals(Double.NaN, fromSpanishFloat("NaN"))
        assertEquals(null, fromSpanishFloat(null))
        assertEquals(null, fromSpanishFloat("bad")) // non float
    }

    private val adapter = SpanishFloatTypeAdapter()

    @Test
    fun `type adapter reads proper floats`() {
        val reader = JsonReader(java.io.StringReader("4.5"))

        val result = adapter.read(reader)

        assertEquals(4.5, result)
    }

    @Test
    fun `type adapter reads invalid float as null`() {
        val reader = JsonReader(java.io.StringReader("\"not a float\""))

        val result = adapter.read(reader)

        assertEquals(null, result)
    }

    @Ignore("It doesn't anymore, remove if this is it")
    @Test
    fun `type adapter reads valid spanish floats`() {
        val reader = JsonReader(java.io.StringReader("\"3,4\""))
        val expected = 3.4

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
        val input = 4.5

        adapter.write(jsonWriter, input)
        jsonWriter.flush()

        assertEquals("\"4,5\"", writer.toString())
    }
}
