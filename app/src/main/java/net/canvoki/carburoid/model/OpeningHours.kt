package net.canvoki.carburoid.model

import java.time.DayOfWeek
import java.time.LocalTime

class OpeningHours() {
    private var value : String = ""
    private val intervals = mutableListOf<Pair<LocalTime, LocalTime>>()


    fun serialize(): String {
        if (intervals.isEmpty()) return ""
        val intervalStrings = intervals.map { (start, end) ->
            "${formatTime(start)}-${formatTime(end)}"
        }

        return "L: ${intervalStrings.joinToString(" y ")}"
    }
    fun add(day: DayOfWeek, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        intervals.add(
            LocalTime.of(startHour, startMinute) to LocalTime.of(endHour, endMinute)
        )
        if (value!="") {
            value += " y %02d:%02d-%02d:%02d".format(startHour, startMinute, endHour, endMinute)
            return
        }
        value = "L: %02d:%02d-%02d:%02d".format(startHour, startMinute, endHour, endMinute)
    }
    private fun formatTime(time: LocalTime): String {
        return String.format("%02d:%02d", time.hour, time.minute)
    }
}
