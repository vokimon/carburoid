package net.canvoki.carburoid.test

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

val BASE_MONDAY = Instant.parse("2025-09-01T00:00:00Z") // Monday
val MADRID_ZONE = ZoneId.of("Europe/Madrid")

/**
 * Returns the ISO Zulu string of the the day at localtime in Madrid
 * considering arbitrarily the first monday Sep 1 2025.
 */
fun madridInstant(
    day: DayOfWeek,
    localtime: String,
    weekOffset: Int = 0,
): String {
    val time = LocalTime.parse(localtime)
    val daysFromBase = (day.ordinal - DayOfWeek.MONDAY.ordinal) + (weekOffset * 7)
    val targetDate =
        BASE_MONDAY
            .atZone(MADRID_ZONE)
            .toLocalDate()
            .plusDays(daysFromBase.toLong())
    val targetLocal = LocalDateTime.of(targetDate, time)
    return targetLocal.atZone(MADRID_ZONE).toInstant().toString()
}

fun <T> atMadridInstant(
    day: DayOfWeek,
    localtime: String,
    weekOffset: Int,
    block: () -> T,
): T {
    val fixedTime = Instant.parse(madridInstant(day, localtime, weekOffset))
    mockkStatic(Instant::class)
    try {
        every { Instant.now() } returns fixedTime
        return block()
    } finally {
        unmockkAll()
    }
}

fun <T> atMadridInstant(
    day: DayOfWeek,
    localtime: String,
    block: () -> T,
): T = atMadridInstant(day, localtime, weekOffset = 0, block)
