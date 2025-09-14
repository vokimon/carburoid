package net.canvoki.carburoid.json

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SpanishFloatTest {
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

    // TODO: Test SpanshFloatTypeAdapter
}
