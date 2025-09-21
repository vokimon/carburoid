package net.canvoki.carburoid.model

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

val END_OF_DAY = LocalTime.of(23, 59)
typealias TimeSpec = Pair<Int,Int>
typealias Interval = Pair<TimeSpec, TimeSpec>
typealias Intervals = List<Interval>
typealias DayRange = List<DayOfWeek>
typealias ScheduleEntry = Pair<DayRange, Intervals>

data class OpeningStatus(
    val isOpen: Boolean,
    val nextChange: Instant?,
)

fun toLocal(instant: Instant, zoneId: ZoneId = ZoneId.of("Europe/Madrid")): Pair<DayOfWeek, LocalTime> {
    val localDateTime = instant.atZone(zoneId).toLocalDateTime()
    val truncatedTime = localDateTime.toLocalTime().truncatedTo(ChronoUnit.MINUTES)
    return localDateTime.dayOfWeek to truncatedTime
}



fun toInstant(reference: Instant, day: DayOfWeek, time: LocalTime, zoneId: ZoneId): Instant {
    val refZoned = reference.atZone(zoneId)
    val refDate = refZoned.toLocalDate()
    val refDay = refZoned.dayOfWeek
    val refDayValue = refDay.value
    val refTime = refZoned.toLocalTime()
    val targetDayValue = day.value

    val daysToAdd = if (day==refDay && time < refTime) 7 else {
        (targetDayValue - refDayValue + 7) % 7
    }

    val targetDate = refDate.plusDays(daysToAdd.toLong())
    val targetLocal = LocalDateTime.of(targetDate, time)
    return targetLocal.atZone(zoneId).toInstant()
}

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

    fun getStatus(instant: Instant, zoneId: ZoneId): OpeningStatus {
        if (dayIntervals.isEmpty())
            return OpeningStatus(false, null)
        return OpeningStatus(true, null)
    }

    override fun toString(): String {
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

        fun parseTime(intervalStr: String): TimeSpec? {
            val parts = intervalStr.split(":")
            if (parts.size != 2) return null

            val hours = parts[0].toIntOrNull() ?: return null
            val minutes= parts[1].toIntOrNull() ?: return null

            if (hours !in 0..23) return null
            if (minutes !in 0..59) return null

            return hours to minutes
        }

        fun parseInterval(intervalStr: String): Interval? {
            if (intervalStr == "24H") return ((0 to 0) to (23 to 59))
            val parts = intervalStr.split("-")
            if (parts.size != 2) return null

            val start = parseTime(parts[0]) ?: return null
            val end = parseTime(parts[1]) ?: return null

            if (start.first > end.first) return null
            if (start.first == end.first) {
                if (start.second >= end.second) return null
            }

            return start to end
        }

        fun parseIntervals(spec: String): Intervals? {
            val parts = spec.split(" y ")
            val intervals = parts.map { parseInterval(it) }
            if (intervals.any { it == null }) return null
            return intervals.map { it!! }
        }

        fun parseDayShort(spec: String): DayOfWeek? {
            return when (spec) {
                "L" -> DayOfWeek.MONDAY
                "M" -> DayOfWeek.TUESDAY
                "X" -> DayOfWeek.WEDNESDAY
                "J" -> DayOfWeek.THURSDAY
                "V" -> DayOfWeek.FRIDAY
                "S" -> DayOfWeek.SATURDAY
                "D" -> DayOfWeek.SUNDAY
                else -> null
            }
        }

        fun parseDayRange(spec: String): DayRange? {
            val parts = spec.split("-")
            val start = parts.getOrNull(0)?.let { parseDayShort(it) }
            if (start==null) return null

            if (parts.size == 1) return listOf(start)

            val end = parts[1]?.let { parseDayShort(it) }
            val allDays = DayOfWeek.values().toList()
            val startIndex = allDays.indexOf(start)
            val endIndex = allDays.indexOf(end)
            if (startIndex < endIndex)
                return allDays.subList(startIndex, endIndex+1)
            // Crossed? cycle through end
            return allDays.subList(startIndex, allDays.size) + allDays.subList(0, endIndex+1)
        }

        fun parseScheduleEntry(spec: String): ScheduleEntry? {
            val parts = spec.split(": ")
            if (parts.size != 2) return null
            val (daysStr, timesStr) = parts
            val days = parseDayRange(daysStr) ?: return null
            val times = parseIntervals(timesStr) ?: return null
            return days to times
        }

        fun parse(spec: String): OpeningHours? {
            val oh = OpeningHours()
            val entries = spec.split("; ")
            for (entrySpec in entries) {
                println("Entry: '$entrySpec'")
                val (days, times) = parseScheduleEntry(entrySpec) ?: return null
                for (day in days) {
                    for (time in times) {
                        val (start, end) = time
                        val (sh, sm) = start
                        val (eh, em) = end
                        oh.add(day, sh, sm, eh, em)
                    }
                }
                continue
            }
            return oh
        }
    }
}
