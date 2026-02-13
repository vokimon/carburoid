package net.canvoki.carburoid.json

import net.canvoki.shared.test.assertEquals
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
    fun `preprocessSpanishNumbers with many numbers`() {
        assertEquals("\"-23.323\", \"12.3455\"", preprocessSpanishNumbers("\"-23,323\", \"12,3455\""))
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
    fun `postprocessSpanishNumbers with many numbers`() {
        assertEquals("\"-23,323\", \"12,3455\"", postprocessSpanishNumbers("\"-23.323\", \"12.3455\""))
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
}
