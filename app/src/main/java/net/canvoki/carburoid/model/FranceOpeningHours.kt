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

        @VisibleForTesting
        fun parseFrenchDayElement(spec: String): ScheduleEntry? {
            if (spec.isEmpty()) return null

            // Handle special Automate case
            if (spec == "Automate-24-24") {
                return DayOfWeek.values().toList() to listOf((0 to 0) to (23 to 59))
            }

            var alphaEnd = 0
            while (alphaEnd < spec.length && spec[alphaEnd].isLetter()) {
                alphaEnd++
            }

            if (alphaEnd == 0) return null // No alphabetic prefix

            val weekdayPart = spec.substring(0, alphaEnd)
            val timePart = spec.substring(alphaEnd).trim()

            val day = parseFrenchWeekday(weekdayPart) ?: return null

            if (timePart.isEmpty()) {
                return listOf(day) to emptyList()
            }

            val intervalParts = timePart.split(" et ")
            if (intervalParts.size > 2) return null
            val intervals = intervalParts.map { parseFrenchInterval(it) ?: return null }
            return listOf(day) to intervals
        }

        fun parse(spec: String): OpeningHours? {
            val oh = FranceOpeningHours()
            for (element in spec.split(",")) {
                val (days, intervals) = parseFrenchDayElement(element.trim()) ?: return null
                for (day in days) {
                    for (interval in intervals) {
                        val (firstHour, firstMinutes) = interval.first
                        val (secondHour, secondMinutes) = interval.second
                        if (firstHour < secondHour || (firstHour == secondHour && firstMinutes < secondMinutes)) {
                            oh.add(
                                day,
                                firstHour,
                                firstMinutes,
                                secondHour,
                                secondMinutes,
                            )
                            continue
                        }
                        oh.add(
                            day,
                            firstHour,
                            firstMinutes,
                            23,
                            59,
                        )
                        if (secondHour != 0 && secondMinutes != 0) {
                            oh.add(
                                day + 1,
                                0,
                                0,
                                secondHour,
                                secondMinutes,
                            )
                        }
                    }
                }
            }
            return oh
        }
    }
}
