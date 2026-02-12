package net.canvoki.carburoid.model

import androidx.annotation.VisibleForTesting
import java.time.DayOfWeek

class FranceOpeningHours : OpeningHours() {
    companion object {
        @VisibleForTesting
        fun parseFrenchWeekday(spec: String): DayOfWeek? =
            when (spec) {
                "Lundi" -> DayOfWeek.MONDAY
                "Mardi" -> DayOfWeek.TUESDAY
                "Mercredi" -> DayOfWeek.WEDNESDAY
                "Jeudi" -> DayOfWeek.THURSDAY
                "Vendredi" -> DayOfWeek.FRIDAY
                "Samedi" -> DayOfWeek.SATURDAY
                "Dimanche" -> DayOfWeek.SUNDAY
                else -> null
            }

        @VisibleForTesting
        fun parseFrenchTime(spec: String): TimeSpec? {
            if (spec.isEmpty()) return null

            val parts = spec.split('.')
            if (parts.size != 2) return null

            val hours = parts[0].toIntOrNull() ?: return null
            val minutes = parts[1].toIntOrNull() ?: return null

            if (hours !in 0..23) return null
            if (minutes !in 0..59) return null

            return hours to minutes
        }

        @VisibleForTesting
        fun parseFrenchInterval(spec: String): Interval? {
            if (spec.isEmpty()) return null

            val parts = spec.split('-')
            if (parts.size != 2) return null

            val start = parseFrenchTime(parts[0]) ?: return null
            val end = parseFrenchTime(parts[1]) ?: return null

            return start to end
        }
    }
}
