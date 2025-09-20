package net.canvoki.carburoid.model

import java.time.DayOfWeek
import java.time.LocalTime

class OpeningHours() {
    var value : String = ""

    fun serialize(): String {
        return value
    }
    fun add(day: DayOfWeek, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        value = "L: %02d:%02d-%02d:%02d".format(startHour, startMinute, endHour, endMinute)
    }
}
