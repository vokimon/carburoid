package net.canvoki.carburoid.model

import net.canvoki.carburoid.test.madridInstant
import org.junit.Ignore
import org.junit.Test
import org.junit.Before
import org.junit.After
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.fail

class OpeningHoursTest {

    private lateinit var originalLocale: Locale

    @Before
    fun saveLocale() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.ROOT)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `toString for no range`() {
        val openingHours = OpeningHours()

        val result = openingHours.toString()

        assertEquals("", result)
    }

    @Test
    fun `toString for single day single range`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 8, 0, 13, 30)

        val result = openingHours.toString()

        assertEquals("L: 08:00-13:30", result)
    }

    @Test
    fun `toString uses neutral formatting regardless of system locale`() {
        Locale.setDefault(Locale("ar")) // Arabic

        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 8, 0, 13, 30)

        val result = openingHours.toString()

        assertEquals("L: 08:00-13:30", result)
    }

    @Test
    fun `toString for single day different time`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 7, 10, 14, 3)

        val result = openingHours.toString()

        assertEquals("L: 07:10-14:03", result)
    }

    @Test
    fun `toString for Monday with two intervals`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 8, 0, 13, 30)
        openingHours.add(DayOfWeek.MONDAY, 15, 30, 20, 0)

        val result = openingHours.toString()

        assertEquals("L: 08:00-13:30 y 15:30-20:00", result)
    }

    @Test
    fun `toString for Monday with two out of order intervals`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 15, 30, 20, 0)
        openingHours.add(DayOfWeek.MONDAY, 8, 0, 13, 30)

        val result = openingHours.toString()

        assertEquals("L: 08:00-13:30 y 15:30-20:00", result)
    }

    @Test
    fun `toString later interval overlaps the start of the existing one`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 14, 0, 20, 0)
        openingHours.add(DayOfWeek.MONDAY, 10, 0, 16, 0)

        val result = openingHours.toString()

        assertEquals("L: 10:00-20:00", result)
    }

    @Test
    fun `toString later interval within the existing one`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 10, 0, 20, 0)
        openingHours.add(DayOfWeek.MONDAY, 14, 0, 16, 0)

        val result = openingHours.toString()

        assertEquals("L: 10:00-20:00", result)
    }

    @Test
    fun `toString later interval overlaps the end of the existing one`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 10, 0, 16, 0)
        openingHours.add(DayOfWeek.MONDAY, 14, 0, 20, 0)

        val result = openingHours.toString()

        assertEquals("L: 10:00-20:00", result)
    }

    @Test
    fun `toString later interval joins existing ones`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 10, 0, 14, 0)
        openingHours.add(DayOfWeek.MONDAY, 16, 0, 20, 0)
        openingHours.add(DayOfWeek.MONDAY, 12, 0, 18, 0)

        val result = openingHours.toString()

        assertEquals("L: 10:00-20:00", result)
    }

    @Test
    fun `toString keep intervals before the merge`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 4, 0, 5, 0) // not involved
        openingHours.add(DayOfWeek.MONDAY, 10, 0, 14, 0)
        openingHours.add(DayOfWeek.MONDAY, 16, 0, 20, 0)
        openingHours.add(DayOfWeek.MONDAY, 12, 0, 18, 0)

        val result = openingHours.toString()

        assertEquals("L: 04:00-05:00 y 10:00-20:00", result)
    }

    @Test
    fun `toString keep intervals after the merge`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 10, 0, 14, 0)
        openingHours.add(DayOfWeek.MONDAY, 16, 0, 20, 0)
        openingHours.add(DayOfWeek.MONDAY, 12, 0, 18, 0)
        openingHours.add(DayOfWeek.MONDAY, 21, 0, 22, 0) // not involved

        val result = openingHours.toString()

        assertEquals("L: 10:00-20:00 y 21:00-22:00", result)
    }

    @Test
    fun `toString for single day 24H`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 0, 0, 23, 59)

        val result = openingHours.toString()

        assertEquals("L: 24H", result)
    }

    @Test
    fun `toString Tuesday single interval`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.TUESDAY, 10, 0, 14, 0)

        val result = openingHours.toString()

        assertEquals("M: 10:00-14:00", result)
    }

    @Test
    fun `toString Wenesday single interval`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.WEDNESDAY, 10, 0, 14, 0)

        val result = openingHours.toString()

        assertEquals("X: 10:00-14:00", result)
    }

    @Test
    fun `toString Thursday single interval`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.THURSDAY, 10, 0, 14, 0)

        val result = openingHours.toString()

        assertEquals("J: 10:00-14:00", result)
    }

    @Test
    fun `toString Friday single interval`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.FRIDAY, 10, 0, 14, 0)

        val result = openingHours.toString()

        assertEquals("V: 10:00-14:00", result)
    }

    @Test
    fun `toString Saturday single interval`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.SATURDAY, 10, 0, 14, 0)

        val result = openingHours.toString()

        assertEquals("S: 10:00-14:00", result)
    }

    @Test
    fun `toString Sunday single interval`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.SUNDAY, 10, 0, 14, 0)

        val result = openingHours.toString()

        assertEquals("D: 10:00-14:00", result)
    }

    @Test
    fun `toString consecutive days with different intervals`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 8, 0, 13, 30)
        openingHours.add(DayOfWeek.TUESDAY, 10, 0, 13, 30)

        val result = openingHours.toString()

        assertEquals("L: 08:00-13:30; M: 10:00-13:30", result)
    }

    @Test
    fun `toString collapses two consecutive days with identical intervals into day range`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 8, 0, 13, 30)
        openingHours.add(DayOfWeek.TUESDAY, 8, 0, 13, 30)

        val result = openingHours.toString()

        assertEquals("L-M: 08:00-13:30", result)
    }

    @Test
    fun `toString does not collapse equal intervals in separated days`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 8, 0, 13, 30)
        openingHours.add(DayOfWeek.WEDNESDAY, 8, 0, 13, 30)

        val result = openingHours.toString()

        assertEquals("L: 08:00-13:30; X: 08:00-13:30", result)
    }

    @Test
    fun `toString more than two equal intervals in separated days`() {
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 8, 0, 13, 30)
        openingHours.add(DayOfWeek.TUESDAY, 8, 0, 13, 30)
        openingHours.add(DayOfWeek.WEDNESDAY, 8, 0, 13, 30)

        val result = openingHours.toString()

        assertEquals("L-X: 08:00-13:30", result)
    }

    fun parseTimeTestCase(expected: TimeSpec?, spec: String) {
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

    // parseInterval (single)

    fun parseIntervalTestCase(expected: Interval?, spec: String) {
        val result = OpeningHours.parseInterval(spec)
        assertEquals(expected, result)
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
        parseIntervalTestCase(null, "12:34-bad")
    }

    @Test
    fun `parseInterval crossed interval hours`() {
        parseIntervalTestCase((12 to 34) to (2 to 30), "12:34-02:30")
    }

    @Test
    fun `parseInterval crossed interval minutes, considered valid`() {
        parseIntervalTestCase((12 to 34) to (12 to 30), "12:34-12:30")
    }

    @Test
    fun `parseInterval proper interval, considered valid`() {
        parseIntervalTestCase((12 to 34) to (22 to 30), "12:34-22:30")
    }

    // parseIntervals (multiple)

    fun parseIntervalsTestCase(expected: Intervals?, spec: String) {
        val result = OpeningHours.parseIntervals(spec)
        assertEquals(expected, result)
    }

    @Test
    fun `parseIntervals single interval`() {
        parseIntervalsTestCase(
            listOf(
                (12 to 34) to (22 to 30),
            ),
            "12:34-22:30",
        )
    }

    @Test
    fun `parseIntervals multiple interval`() {
        parseIntervalsTestCase(
            listOf(
                (0 to 0) to (10 to 0),
                (12 to 34) to (22 to 30),
            ),
            "00:00-10:00 y 12:34-22:30",
        )
    }

    @Test
    fun `parseIntervals bad interval`() {
        parseIntervalsTestCase(null, "BAD")
    }

    // parseDayShort

    fun parseDayShort_testCase(expected: DayOfWeek?, spec: String) {
        assertEquals(expected, OpeningHours.parseDayShort(spec))
    }

    @Test
    fun `parseDayShort L returns MONDAY`() {
        parseDayShort_testCase(DayOfWeek.MONDAY, "L")
    }

    @Test
    fun `parseDayShort BAD returns null`() {
        parseDayShort_testCase(null, "BAD")
    }

    @Test
    fun `parseDayShort M returns TUESDAY`() {
        parseDayShort_testCase(DayOfWeek.TUESDAY, "M")
    }

    @Test
    fun `parseDayShort X returns WEDNESDAY`() {
        parseDayShort_testCase(DayOfWeek.WEDNESDAY, "X")
    }

    @Test
    fun `parseDayShort J returns THURSDAY`() {
        parseDayShort_testCase(DayOfWeek.THURSDAY, "J")
    }

    @Test
    fun `parseDayShort V returns FRIDAY`() {
        parseDayShort_testCase(DayOfWeek.FRIDAY, "V")
    }

    @Test
    fun `parseDayShort S returns SATURDAY`() {
        parseDayShort_testCase(DayOfWeek.SATURDAY, "S")
    }

    @Test
    fun `parseDayShort D returns SUNDAY`() {
        parseDayShort_testCase(DayOfWeek.SUNDAY, "D")
    }

    fun parseDayRange_testCase(expected: DayRange?, spec: String) {
        val result = OpeningHours.parseDayRange(spec)
        assertEquals(expected, result)
    }

    @Test
    fun `parseDayRange single day wraps in list`() {
        parseDayRange_testCase(
            listOf(
                DayOfWeek.TUESDAY,
            ),
            "M",
        )
    }

    @Test
    fun `parseDayRange bad day returns null`() {
        parseDayRange_testCase(null, "BAD")
    }

    @Test
    fun `parseDayRange consecutiive bounds`() {
        parseDayRange_testCase(
            listOf(
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
            ),
            "M-X",
        )
    }

    @Test
    fun `parseDayRange non-consecutive bounds`() {
        parseDayRange_testCase(
            listOf(
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
            ),
            "M-J",
        )
    }

    @Test
    fun `parseDayRange inverted order cycles`() {
        parseDayRange_testCase(
            listOf(
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
            ),
            "J-M",
        )
    }

    // parseScheduleEntry

    fun parseScheduleEntry_TestCase(expected: ScheduleEntry?, spec: String) {
        assertEquals(expected, OpeningHours.parseScheduleEntry(spec))
    }

    @Test
    fun `parseScheduleEntry no colon`() {
        parseScheduleEntry_TestCase(null, "no colon")
    }

    @Test
    fun `parseScheduleEntry bad days`() {
        parseScheduleEntry_TestCase(null, "BAD: 14:00-22:00")
    }

    @Test
    fun `parseScheduleEntry bad times`() {
        parseScheduleEntry_TestCase(null, "M-J: BAD")
    }

    @Test
    fun `parseScheduleEntry proper`() {
        parseScheduleEntry_TestCase(
            listOf(
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
            ) to listOf(
                (3 to 0) to (12 to 0),
                (14 to 0) to (22 to 0),
            ),
            "V-S: 3:00-12:00 y 14:00-22:00",
        )
    }

    // parse
    fun parse_TestCase(expected: String?, spec: String) {
        assertEquals(expected, OpeningHours.parse(spec)?.toString())
    }

    @Test
    fun `parse single entry`() {
        parse_TestCase("L-M: 08:00-13:30", "L-M: 08:00-13:30")
    }

    @Test
    fun `parse multiple entries`() {
        parse_TestCase("L-M: 08:00-13:30; V: 24H", "L-M: 08:00-13:30; V: 24H")
    }

    @Test
    fun `parse bad entry`() {
        parse_TestCase(null, "L-M: 08:00-13:30; V: BAD")
    }

    @Test
    fun `parse crossing hours expands on two days`() {
        parse_TestCase("L: 22:00-23:59; M: 00:00-02:00", "L: 22:00-02:00")
    }

    @Test
    fun `parse crossing hours expansion on Sunday cycles to Monday`() {
        parse_TestCase("L: 00:00-02:00; D: 22:00-23:59", "D: 22:00-02:00")
    }

    @Test
    fun `parse ending in 0h moved to 23_59`() {
        parse_TestCase("L: 22:00-23:59", "L: 22:00-00:00")
    }

    // getStatus

    fun getStatus_testCase(
        openings: String,
        at: String,
        isOpen: Boolean,
        until: String?,
        isCanaryIslands: Boolean = false,
    ) {
        val oh = OpeningHours.parse(openings) ?: OpeningHours()
        val instant = Instant.parse(at)
        val zoneId = ZoneId.of(if (isCanaryIslands) "Atlantic/Canary" else "Europe/Madrid")
        val status = oh.getStatus(instant, zoneId)
        val expectedNextChange = until?.let { Instant.parse(it) }
        assertEquals(
            OpeningStatus(isOpen = isOpen, until = expectedNextChange),
            status,
        )
    }

    @Test
    fun `getStatus always closed never opens`() {
        getStatus_testCase(
            openings = "",
            at = "2025-06-09T00:00:00Z",
            isOpen = false,
            until = null,
        )
    }

    @Test
    fun `getStatus single full day within that day, open till next day`() {
        getStatus_testCase(
            openings = "M: 24H", // Tuesday full day
            at = madridInstant(DayOfWeek.TUESDAY, "12:00"),
            isOpen = true,
            until = madridInstant(DayOfWeek.WEDNESDAY, "00:00"),
        )
    }

    @Test
    fun `getStatus before a single full day, closed until that day`() {
        getStatus_testCase(
            openings = "M: 24H", // Tuesday full day
            at = madridInstant(DayOfWeek.MONDAY, "12:00"),
            isOpen = false,
            until = madridInstant(DayOfWeek.TUESDAY, "00:00"),
        )
    }

    @Test
    fun `getStatus days before a single full day, closed until that day`() {
        getStatus_testCase(
            openings = "X: 24H", // Wednesday full day
            at = madridInstant(DayOfWeek.MONDAY, "12:00"),
            isOpen = false,
            until = madridInstant(DayOfWeek.WEDNESDAY, "00:00"),
        )
    }

    @Test
    fun `getStatus after a single full day, closed until that day next week`() {
        getStatus_testCase(
            openings = "X: 24H", // Wednesday full day
            at = madridInstant(DayOfWeek.THURSDAY, "12:00"),
            isOpen = false,
            until = madridInstant(DayOfWeek.WEDNESDAY, "00:00", weekOffset = 1),
        )
    }

    @Test
    fun `getStatus single partial day within that opening, open till end of opening`() {
        getStatus_testCase(
            openings = "M: 8:00-20:00", // Tuesday partial day
            at = madridInstant(DayOfWeek.TUESDAY, "12:00"),
            isOpen = true,
            until = madridInstant(DayOfWeek.TUESDAY, "20:00"),
        )
    }

    @Test
    fun `getStatus single partial day, same day before that opening, closed till start of opening`() {
        getStatus_testCase(
            openings = "M: 8:00-20:00", // Tuesday partial day
            at = madridInstant(DayOfWeek.TUESDAY, "07:00"),
            isOpen = false,
            until = madridInstant(DayOfWeek.TUESDAY, "08:00"),
        )
    }

    @Test
    fun `getStatus days before a single partial day, closed until that day at that time`() {
        getStatus_testCase(
            openings = "M: 8:00-20:00", // Tuesday partial day
            at = madridInstant(DayOfWeek.MONDAY, "12:00"),
            isOpen = false,
            until = madridInstant(DayOfWeek.TUESDAY, "08:00"),
        )
    }

    @Test
    fun `getStatus single partial day, same day after that opening, closed till start of opening next week`() {
        getStatus_testCase(
            openings = "M: 8:00-20:00", // Tuesday partial day
            at = madridInstant(DayOfWeek.TUESDAY, "22:00"),
            isOpen = false,
            until = madridInstant(DayOfWeek.TUESDAY, "08:00", weekOffset = 1),
        )
    }

    @Test
    fun `getStatus in between openings in the same day`() {
        getStatus_testCase(
            openings = "M: 8:00-14:00 y 17:00-20:00", // Tuesday splitted day
            at = madridInstant(DayOfWeek.TUESDAY, "15:00"), // in between
            isOpen = false,
            until = madridInstant(DayOfWeek.TUESDAY, "17:00"),
        )
    }

    @Test
    fun `getStatus contiguous openings close on the second`() {
        getStatus_testCase(
            openings = "M: 8:00-14:59 y 15:00-20:00", // Tuesday contiguous openings within day
            at = madridInstant(DayOfWeek.TUESDAY, "12:00"), // on the first opening
            isOpen = true,
            until = madridInstant(DayOfWeek.TUESDAY, "20:00"), // takes the second closing
        )
    }

    @Test
    fun `getStatus noncontiguous openings close on the first`() {
        getStatus_testCase(
            openings = "M: 8:00-14:00 y 16:00-20:00", // Tuesday separated openings within day
            at = madridInstant(DayOfWeek.TUESDAY, "12:00"), // on the first opening
            isOpen = true,
            until = madridInstant(DayOfWeek.TUESDAY, "14:00"), // takes the first closing
        )
    }

    @Test
    fun `getStatus contiguous openings next day`() {
        getStatus_testCase(
            openings = "M: 8:00-23:59; X: 00:00-10:00", // Tuesday and Wednesday contiguous
            at = madridInstant(DayOfWeek.TUESDAY, "12:00"), // on the first opening
            isOpen = true,
            until = madridInstant(DayOfWeek.WEDNESDAY, "10:00"), // takes the first closing
        )
    }

    @Test
    fun `getStatus noncontiguous openings next day`() {
        getStatus_testCase(
            openings = "M: 8:00-23:59; X: 02:00-10:00", // Tuesday and Wednesday noncontiguous
            at = madridInstant(DayOfWeek.TUESDAY, "12:00"), // on the first opening
            isOpen = true,
            until = madridInstant(DayOfWeek.WEDNESDAY, "00:00"), // takes the first closing
        )
    }

    @Test
    fun `getStatus not ending at midnight breaks continuity`() {
        getStatus_testCase(
            openings = "M: 8:00-23:00; X: 00:00-10:00", // Tuesday and Wednesday noncontiguous
            at = madridInstant(DayOfWeek.TUESDAY, "12:00"), // on the first opening
            isOpen = true,
            until = madridInstant(DayOfWeek.TUESDAY, "23:00"), // takes the first closing
        )
    }

    @Test
    fun `getStatus continuity search follows next interval in next day`() {
        getStatus_testCase(
            openings = "M: 8:00-23:59; X: 00:00-09:59 y 10:00-14:00", // Tuesday and two contiguous on Wednesday
            at = madridInstant(DayOfWeek.TUESDAY, "12:00"), // on the first opening
            isOpen = true,
            until = madridInstant(DayOfWeek.WEDNESDAY, "14:00"), // takes the last closing
        )
    }

    @Test
    fun `getStatus one minute gap is continuity`() {
        getStatus_testCase(
            openings = "M: 8:00-23:59; X: 00:00-09:59 y 10:00-14:00", // Tuesday and two contiguous on Wednesday
            at = madridInstant(DayOfWeek.TUESDAY, "12:00"), // on the first opening
            isOpen = true,
            until = madridInstant(DayOfWeek.WEDNESDAY, "14:00"), // takes the last closing
        )
    }

    @Test
    fun `getStatus more than one minute gap is discontinuity`() {
        getStatus_testCase(
            openings = "M: 8:00-23:59; X: 00:00-09:58 y 10:00-14:00", // Tuesday and noncontiguous Wednesday
            at = madridInstant(DayOfWeek.TUESDAY, "12:00"), // on the first opening
            isOpen = true,
            until = madridInstant(DayOfWeek.WEDNESDAY, "09:58"), // takes the second closing
        )
    }

    @Test
    fun `getStatus openness crossing several days`() {
        getStatus_testCase(
            openings = "M: 8:00-23:59; X: 24H; J: 00:00-14:00", // Tuesday and Wednesday noncontiguous
            at = madridInstant(DayOfWeek.TUESDAY, "12:00"), // on the first opening
            isOpen = true,
            until = madridInstant(DayOfWeek.THURSDAY, "14:00"), // takes the last closing
        )
    }

    @Test
    fun `getStatus far opening`() {
        getStatus_testCase(
            openings = "J: 08:00-14:00", // Thursday
            at = madridInstant(DayOfWeek.TUESDAY, "12:00"), // several days before
            isOpen = false,
            until = madridInstant(DayOfWeek.THURSDAY, "08:00"), // takes the last closing
        )
    }

    @Test
    fun `getStatus 24 7 returns open with no next change`() {
        getStatus_testCase(
            openings = "L-D: 24H",
            at = madridInstant(DayOfWeek.TUESDAY, "12:00"),
            isOpen = true,
            until = null,
        )
    }

    // toLocal toInstant helpers

    @Test
    fun `toLocal applies tz trucates to minutes`() {
        assertEquals(
            DayOfWeek.MONDAY to LocalTime.parse("12:04")!!,
            toLocal(Instant.parse("2025-09-01T10:04:33Z")!!),
        )
    }

    val wednesdayAtNonMadrid = Instant.parse("2025-09-03T10:04:00Z")

    @Test
    fun `toInstant same day later time just changes time`() {
        assertEquals(
            Instant.parse("2025-09-03T10:04:00Z"),
            toInstant(
                reference = wednesdayAtNonMadrid,
                day = DayOfWeek.WEDNESDAY,
                time = LocalTime.parse("12:04"),
                zoneId = ZoneId.of("Europe/Madrid"),
            ),
        )
    }

    @Test
    fun `toInstant next day`() {
        assertEquals(
            Instant.parse("2025-09-04T10:04:00Z"),
            toInstant(
                reference = wednesdayAtNonMadrid,
                day = DayOfWeek.THURSDAY,
                time = LocalTime.parse("12:04"),
                zoneId = ZoneId.of("Europe/Madrid"),
            ),
        )
    }

    @Test
    fun `toInstant next week`() {
        assertEquals(
            Instant.parse("2025-09-08T10:04:00Z"),
            toInstant(
                reference = wednesdayAtNonMadrid,
                day = DayOfWeek.MONDAY,
                time = LocalTime.parse("12:04"),
                zoneId = ZoneId.of("Europe/Madrid"),
            ),
        )
    }

    @Test
    fun `toInstant same day earlier`() {
        assertEquals(
            Instant.parse("2025-09-10T08:04:00Z"),
            toInstant(
                reference = wednesdayAtNonMadrid,
                day = DayOfWeek.WEDNESDAY,
                time = LocalTime.parse("10:04"),
                zoneId = ZoneId.of("Europe/Madrid"),
            ),
        )
    }

    @Ignore("Enable just to detect production data failures")
    @Test
    fun `round trip parse-serialize matches original for all specs in file`() {
        // Populate the file with
        // $ curl -s "https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/" | jq -r '..|.Horario? // empty' | sort | uniq  > app/src/test/resources/opening_hours_specs.txt
        val file = File("src/test/resources/opening_hours_specs.txt")
        val lines = file.readLines()

        val failures = mutableListOf<String>()

        for (line in lines) {
            if (line.isBlank()) continue

            try {
                val oh = OpeningHours.parse(line)
                val serialized = oh.toString()

                if (serialized != line) {
                    failures.add("Original: '$line' → Serialized: '$serialized'")
                }
            } catch (e: Exception) {
                failures.add("Failed to parse: '$line' → ${e.message}")
            }
        }

        if (failures.isNotEmpty()) {
            println("Round-trip failures:")
            failures.forEach { println("  $it") }
            fail("Round-trip failed for ${failures.size} specs")
        }
    }
}
