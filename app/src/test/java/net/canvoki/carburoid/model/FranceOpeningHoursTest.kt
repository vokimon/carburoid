package net.canvoki.carburoid.model

import net.canvoki.carburoid.test.assertEquals
import net.canvoki.carburoid.test.madridInstant
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import kotlin.test.fail

class FranceOpeningHoursTest {
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

    private fun parseFrenchWeekdayTestCase(
        expected: DayOfWeek?,
        spec: String,
    ) {
        val result = FranceOpeningHours.parseFrenchWeekday(spec)
        assertEquals(expected, result)
    }

    @Test
    fun `parseFrenchWeekday returns null for empty string`() {
        parseFrenchWeekdayTestCase(null, "")
    }

    @Test
    fun `parseFrenchWeekday returns null for unknown day`() {
        parseFrenchWeekdayTestCase(null, "Unknown")
    }

    @Test
    fun `parseFrenchWeekday Dimanche`() {
        parseFrenchWeekdayTestCase(DayOfWeek.SUNDAY, "Dimanche")
    }

    @Test
    fun `parseFrenchWeekday Lundi`() {
        parseFrenchWeekdayTestCase(DayOfWeek.MONDAY, "Lundi")
    }

    @Test
    fun `parseFrenchWeekday Mardi`() {
        parseFrenchWeekdayTestCase(DayOfWeek.TUESDAY, "Mardi")
    }

    @Test
    fun `parseFrenchWeekday Mercredi`() {
        parseFrenchWeekdayTestCase(DayOfWeek.WEDNESDAY, "Mercredi")
    }

    @Test
    fun `parseFrenchWeekday parses Jeudi`() {
        parseFrenchWeekdayTestCase(DayOfWeek.THURSDAY, "Jeudi")
    }

    @Test
    fun `parseFrenchWeekday parses Vendredi`() {
        parseFrenchWeekdayTestCase(DayOfWeek.FRIDAY, "Vendredi")
    }

    @Test
    fun `parseFrenchWeekday parses Samedi`() {
        parseFrenchWeekdayTestCase(DayOfWeek.SATURDAY, "Samedi")
    }

    private fun parseFrenchTimeTestCase(
        expected: TimeSpec?,
        spec: String,
    ) {
        val result = FranceOpeningHours.parseFrenchTime(spec)
        assertEquals(expected, result)
    }

    @Test
    fun `parseFrenchTime returns null for empty string`() {
        parseFrenchTimeTestCase(null, "")
    }

    @Test
    fun `parseFrenchTime returns null for missing dot`() {
        parseFrenchTimeTestCase(null, "1230")
    }

    @Test
    fun `parseFrenchTime returns null for colon separator`() {
        parseFrenchTimeTestCase(null, "12:30")
    }

    @Test
    fun `parseFrenchTime returns null for non-numeric hours`() {
        parseFrenchTimeTestCase(null, "ab.30")
    }

    @Test
    fun `parseFrenchTime returns null for non-numeric minutes`() {
        parseFrenchTimeTestCase(null, "12.cd")
    }

    @Test
    fun `parseFrenchTime returns null for hour out of range`() {
        parseFrenchTimeTestCase(null, "24.30")
    }

    @Test
    fun `parseFrenchTime returns null for negative hour`() {
        parseFrenchTimeTestCase(null, "-1.30")
    }

    @Test
    fun `parseFrenchTime returns null for minute out of range`() {
        parseFrenchTimeTestCase(null, "12.60")
    }

    @Test
    fun `parseFrenchTime parses 08 10`() {
        parseFrenchTimeTestCase(8 to 10, "08.10")
    }

    @Test
    fun `parseFrenchTime parses 13 30`() {
        parseFrenchTimeTestCase(13 to 30, "13.30")
    }

    @Test
    fun `parseFrenchTime parses 20 00`() {
        parseFrenchTimeTestCase(20 to 0, "20.00")
    }

    @Test
    fun `parseFrenchTime parses 00 00`() {
        parseFrenchTimeTestCase(0 to 0, "00.00")
    }

    @Test
    fun `parseFrenchTime parses single digit hour`() {
        parseFrenchTimeTestCase(8 to 10, "8.10")
    }

    @Test
    fun `parseFrenchTime parses single digit minute`() {
        parseFrenchTimeTestCase(13 to 3, "13.3")
    }

    @Test
    fun `parseFrenchTime parses single digit both`() {
        parseFrenchTimeTestCase(8 to 3, "8.3")
    }

    private fun parseFrenchIntervalTestCase(
        expected: Interval?,
        spec: String,
    ) {
        val result = FranceOpeningHours.parseFrenchInterval(spec)
        assertEquals(expected, result)
    }

    @Test
    fun `parseFrenchInterval returns null for empty string`() {
        parseFrenchIntervalTestCase(null, "")
    }

    @Test
    fun `parseFrenchInterval returns null for missing dash`() {
        parseFrenchIntervalTestCase(null, "08.10")
    }

    @Test
    fun `parseFrenchInterval returns null for bad start time`() {
        parseFrenchIntervalTestCase(null, "bad-20.00")
    }

    @Test
    fun `parseFrenchInterval returns null for bad end time`() {
        parseFrenchIntervalTestCase(null, "08.10-bad")
    }

    @Test
    fun `parseFrenchInterval parses single interval 08_10-20_00`() {
        parseFrenchIntervalTestCase((8 to 10) to (20 to 0), "08.10-20.00")
    }

    @Test
    fun `parseFrenchInterval parses single interval 13_30-20_20`() {
        parseFrenchIntervalTestCase((13 to 30) to (20 to 20), "13.30-20.20")
    }

    @Test
    fun `parseFrenchInterval handles midnight end as 00_00`() {
        parseFrenchIntervalTestCase((8 to 0) to (0 to 0), "08.00-00.00")
    }

    @Test
    fun `parseFrenchInterval handles explicit 23_59 end`() {
        parseFrenchIntervalTestCase((8 to 0) to (23 to 59), "08.00-23.59")
    }

    @Test
    fun `parseFrenchInterval handles next day crossing 01_00`() {
        parseFrenchIntervalTestCase((1 to 0) to (1 to 0), "01.00-01.00")
    }

    @Test
    fun `parseFrenchInterval handles crossing midnight`() {
        parseFrenchIntervalTestCase((22 to 0) to (6 to 0), "22.00-06.00")
    }
}
