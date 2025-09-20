package net.canvoki.carburoid.model

import java.time.DayOfWeek
import java.time.LocalTime

val END_OF_DAY = LocalTime.of(23, 59)

class OpeningHours() {
    private val intervals = mutableListOf<Pair<LocalTime, LocalTime>>()
    private var currentDay: DayOfWeek = DayOfWeek.MONDAY
    private val dayIntervals = mutableMapOf<DayOfWeek, MutableList<Pair<LocalTime, LocalTime>>>()

    fun add(day: DayOfWeek, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        val interval = LocalTime.of(startHour, startMinute) to LocalTime.of(endHour, endMinute)
        intervals.add(interval)
        currentDay = day
        dayIntervals.getOrPut(day) { mutableListOf() }.add(interval)
    }

    fun serialize(): String {
        val dayStrings: List<Pair<String,String>> = DayOfWeek.values().map { day ->
            spanishWeekDayShort(day)  to formatIntervals(dayIntervals[day] ?: emptyList())
        }

        var pivot = ""
        val result = mutableListOf<Pair<String,String>>()
        for (window in dayStrings.windowed(2, partialWindows=true)) {
            val (currentDay, currentInterval) = window[0]
            val nextInterval = window.getOrNull(1)?.second
            if (currentInterval.isEmpty()) continue
            if (nextInterval == currentInterval) {
                // repeated interval detected
                // set pivot if not set
                if (pivot.isEmpty())
                    pivot = "${currentDay}-"
                // Do not output yet
                continue
            }
            result.add(pivot+currentDay to currentInterval)
            pivot = "" // reset pivot
        }

        return result
            .map {(day, str) -> "$day: $str" }
            .joinToString("; ")
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

    companion object {
        fun parseTime(intervalStr: String) : Pair<Int,Int>? {
            val parts = intervalStr.split(":")
            if (parts.size != 2) return null
            val hours = parts[0].toIntOrNull() ?: return null
            val minutes= parts[1].toIntOrNull() ?: return null
            return hours to minutes
        }

}
