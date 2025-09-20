package net.canvoki.carburoid.model

import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.test.assertEquals



class OpeningHoursTest {

    @Test
    fun `serialize for no range`() {
        val openingHours = OpeningHours()

        val result = openingHours.serialize()

        assertEquals("", result)
    }

    @Test
    fun `serialize for single day single range`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 8, 0, 13, 30)

        val result = openingHours.serialize()

        assertEquals("L: 08:00-13:30", result)
    }

    @Test
    fun `serialize for single day different time`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 7, 10, 14, 3)

        val result = openingHours.serialize()

        assertEquals("L: 07:10-14:03", result)
    }
}
