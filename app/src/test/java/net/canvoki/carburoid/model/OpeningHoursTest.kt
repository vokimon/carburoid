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
    fun `serialize consecutive days with different intervals`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 8, 0, 13, 30)
        openingHours.add(DayOfWeek.TUESDAY, 10, 0, 13, 30)

        val result = openingHours.serialize()

        assertEquals("L: 08:00-13:30; M: 10:00-13:30", result)
    }

    @Test
    fun `serialize collapses two consecutive days with identical intervals into day range`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 8, 0, 13, 30)
        openingHours.add(DayOfWeek.TUESDAY, 8, 0, 13, 30)

        val result = openingHours.serialize()

        assertEquals("L-M: 08:00-13:30", result)
    }

    @Test
    fun `Serialize does not collapse equal intervals in separated days`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 8, 0, 13, 30)
        openingHours.add(DayOfWeek.WEDNESDAY, 8, 0, 13, 30)

        val result = openingHours.serialize()

        assertEquals("L: 08:00-13:30; X: 08:00-13:30", result)
    }

    @Test
    fun `serialize more than two equal intervals in separated days`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 8, 0, 13, 30)
        openingHours.add(DayOfWeek.TUESDAY, 8, 0, 13, 30)
        openingHours.add(DayOfWeek.WEDNESDAY, 8, 0, 13, 30)

        val result = openingHours.serialize()

        assertEquals("L-X: 08:00-13:30", result)
    }

    fun parseTimeTestCase(expected: Pair<Int, Int>?, spec: String) {
        val time = OpeningHours.parseTime(spec)
        assertEquals(expected, time)
    }

    @Test
    fun `parseTime without colon`() {
        parseTimeTestCase(null, "nocolon")
    }

    @Test
    fun `parseTime non numerical hours`() {
        parseTimeTestCase(null, "hh:12")
    }

    @Test
    fun `parseTime non numerical minutes`() {
        parseTimeTestCase(null, "12:mm")
    }

    @Test
    fun `parseTime beyond range hours`() {
        parseTimeTestCase(null, "24:12")
    }

    @Test
    fun `parseTime below range hours`() {
        parseTimeTestCase(null, "-1:12")
    }

    @Test
    fun `parseTime beyond range minutes`() {
        parseTimeTestCase(null, "12:-1")
    }

    @Test
    fun `parseTime below range minutes`() {
        parseTimeTestCase(null, "12:60")
    }

    @Test
    fun `parseTime upper bound`() {
        parseTimeTestCase(23 to 59, "23:59")
    }

    @Test
    fun `parseTime lower bound`() {
        parseTimeTestCase(0 to 0, "00:00")
    }

    @Test
    fun `parseTime upadded`() {
        parseTimeTestCase(1 to 2, "1:2")
    }

    @Test
    fun `parseTime zero padded`() {
        parseTimeTestCase(1 to 2, "01:02")
    }

    fun parseIntervalTestCase(expected: Pair<Pair<Int, Int>, Pair<Int, Int>>?, spec: String) {
        val time = OpeningHours.parseInterval(spec)
        assertEquals(expected, time)
    }

    @Test
    fun `parseInterval no dash`() {
        parseIntervalTestCase(null, "nodash")
    }

    @Test
    fun `parseInterval 24H`() {
        parseIntervalTestCase((0 to 0) to (23 to 59), "24H")
    }

    @Test
    fun `parseInterval bad begin`() {
        parseIntervalTestCase(null, "bad-12:34")
    }

    @Test
    fun `parseInterval bad end`() {
        parseIntervalTestCase(null, "12:34-end")
    }

    @Test
    fun `parseInterval crossed interval hours`() {
        parseIntervalTestCase(null, "12:34-02:30")
    }

    @Test
    fun `parseInterval crossed interval minutes`() {
        parseIntervalTestCase(null, "12:34-12:30")
    }

    @Test
    fun `parseInterval proper interval`() {
        parseIntervalTestCase((12 to 34) to (22 to 30), "12:34-22:30")
    }

}
