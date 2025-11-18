package net.canvoki.carburoid.algorithms

import org.junit.Test
import kotlin.test.assertEquals

class FilterConfigTest {
    @Test
    fun `default config hides_expensive_further is true`() {
        val config = FilterConfig()
        assertEquals(true, config.hideExpensiveFurther)
    }
}
