package net.canvoki.carburoid.model

import java.time.DayOfWeek
import java.time.LocalTime

val END_OF_DAY = LocalTime.of(23, 59)

class OpeningHours() {
    private val intervals = mutableListOf<Pair<LocalTime, LocalTime>>()
    var currentDay: DayOfWeek = DayOfWeek.MONDAY

    fun serialize(): String {
        if (intervals.isEmpty()) return ""
        val intervalStrings = intervals.map {formatInterval(it)}

        return "${spanishWeekDayShort(currentDay)}: ${intervalStrings.joinToString(" y ")}"
    }

    fun add(day: DayOfWeek, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        intervals.add(
            LocalTime.of(startHour, startMinute) to LocalTime.of(endHour, endMinute)
        )
        currentDay = day
    }

    private fun formatInterval(interval: Pair<LocalTime, LocalTime>) : String {
        val (start, end) = interval
        if (start == LocalTime.MIDNIGHT && end == END_OF_DAY) {
            return "24H"
        }
        return "${formatTime(start)}-${formatTime(end)}"
    }

    private fun formatTime(time: LocalTime): String {
        return String.format("%02d:%02d", time.hour, time.minute)
    }

    private fun spanishWeekDayShort(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "L"
        DayOfWeek.TUESDAY -> "M"
        DayOfWeek.WEDNESDAY -> "X"
        DayOfWeek.THURSDAY -> "J"
        DayOfWeek.FRIDAY -> "V"
        DayOfWeek.SATURDAY -> "S"
        DayOfWeek.SUNDAY -> "D"
    }
}
