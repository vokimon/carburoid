package net.canvoki.carburoid.model

import java.time.DayOfWeek
import java.time.LocalTime

val END_OF_DAY = LocalTime.of(23, 59)

class OpeningHours() {
    private val intervals = mutableListOf<Pair<LocalTime, LocalTime>>()

    fun serialize(): String {
        if (intervals.isEmpty()) return ""
        val intervalStrings = intervals.map {formatInterval(it)}

        return "L: ${intervalStrings.joinToString(" y ")}"
    }

    fun add(day: DayOfWeek, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        intervals.add(
            LocalTime.of(startHour, startMinute) to LocalTime.of(endHour, endMinute)
        )
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
}
