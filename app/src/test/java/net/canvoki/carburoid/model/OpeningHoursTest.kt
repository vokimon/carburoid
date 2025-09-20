package net.canvoki.carburoid.model

import org.junit.Test
import org.junit.Ignore
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

    @Test
    fun `serialize for Monday with two intervals`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 8, 0, 13, 30)
        openingHours.add(DayOfWeek.MONDAY, 15, 30, 20, 0)

        val result = openingHours.serialize()

        assertEquals("L: 08:00-13:30 y 15:30-20:00", result)
    }

    @Test
    fun `serialize for single day 24H`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 0, 0, 23, 59)

        val result = openingHours.serialize()

        assertEquals("L: 24H", result)
    }

    @Test
    fun `serialize Tuesday single interval`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.TUESDAY, 10, 0, 14, 0)

        val result = openingHours.serialize()

        assertEquals("M: 10:00-14:00", result)
    }

    @Test
    fun `serialize Wenesday single interval`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.WEDNESDAY, 10, 0, 14, 0)

        val result = openingHours.serialize()

        assertEquals("X: 10:00-14:00", result)
    }

    @Test
    fun `serialize Thursday single interval`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.THURSDAY, 10, 0, 14, 0)

        val result = openingHours.serialize()

        assertEquals("J: 10:00-14:00", result)
    }

    @Test
    fun `serialize Friday single interval`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.FRIDAY, 10, 0, 14, 0)

        val result = openingHours.serialize()

        assertEquals("V: 10:00-14:00", result)
    }

    @Test
    fun `serialize Saturday single interval`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.SATURDAY, 10, 0, 14, 0)

        val result = openingHours.serialize()

        assertEquals("S: 10:00-14:00", result)
    }

    @Test
    fun `serialize Sunday single interval`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.SUNDAY, 10, 0, 14, 0)

        val result = openingHours.serialize()

        assertEquals("D: 10:00-14:00", result)
    }

    @Test
    fun `serialize Monday and Wednesday each with one interval`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 8, 0, 13, 30)
        openingHours.add(DayOfWeek.WEDNESDAY, 10, 0, 14, 0)

        val result = openingHours.serialize()

        // Order? We'll assume insertion order for now — API doesn’t specify, so we don’t overengineer
        assertEquals("L: 08:00-13:30; X: 10:00-14:00", result)
    }

}
