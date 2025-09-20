package net.canvoki.carburoid.model

import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.test.assertEquals



class OpeningHoursTest {

    fun timeRange(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): TimeRange {
        return TimeRange(
            LocalTime.of(startHour, startMinute),
            LocalTime.of(endHour, endMinute)
        )
    }

@Test
    fun `serialize for single day single range`() {
        // Given: a schedule with Monday 08:00-13:30
        val schedule = mapOf(
            DayOfWeek.MONDAY to listOf(
                timeRange(8, 0, 13, 30),
            )
        )
        val openingHours = OpeningHours()
        openingHours.add(DayOfWeek.MONDAY, 8, 0, 13, 30)


        // When: we serialize it
        val result = openingHours.serialize()

        // Then: it should match expected format
        assertEquals("L: 08:00-13:30", result)
    }
}
