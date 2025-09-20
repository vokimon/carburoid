package net.canvoki.carburoid.model

import java.time.DayOfWeek
import java.time.LocalTime

val END_OF_DAY = LocalTime.of(23, 59)

class OpeningHours() {
    private val intervals = mutableListOf<Pair<LocalTime, LocalTime>>()
    private var currentDay: DayOfWeek = DayOfWeek.MONDAY
    private val dayIntervals = mutableMapOf<DayOfWeek, MutableList<Pair<LocalTime, LocalTime>>>()

    fun serialize(): String {
        val dayStrings: List<Pair<String,String>> = DayOfWeek.values().map { day ->
            spanishWeekDayShort(day)  to formatIntervals(dayIntervals[day] ?: emptyList())
        }

        if (
            dayStrings[0].first == "L" &&
            dayStrings[1].first == "M" &&
            dayStrings[0].second == dayStrings[1].second &&
            dayStrings[0].second.isNotEmpty()
        ) {
            return "L-M: ${dayStrings[0].second}"
        }

        return dayStrings
            .filter {(_, str) -> str.isNotEmpty() }
            .map {(day, str) -> "$day: $str" }
            .joinToString("; ")
    }

    fun add(day: DayOfWeek, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        val interval = LocalTime.of(startHour, startMinute) to LocalTime.of(endHour, endMinute)
        intervals.add(interval)
        currentDay = day
        dayIntervals.getOrPut(day) { mutableListOf() }.add(interval)
    }

    private fun formatIntervals(intervals: List<Pair<LocalTime, LocalTime>>): String {
        return intervals.map { formatInterval(it) }.joinToString(" y ")
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
