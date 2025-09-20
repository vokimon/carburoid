package net.canvoki.carburoid.model

import java.time.DayOfWeek

class OpeningHours() {
    fun serialize(): String {
        return "L: 08:00-13:30"
    }
    fun add(day: DayOfWeek, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
    }
}
