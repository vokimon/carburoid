package net.canvoki.carburoid.json

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SPANISH_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy H:mm:ss", Locale.ROOT)
private val MADRID_ZONE = ZoneId.of("Europe/Madrid")

fun toSpanishDate(instant: Instant?): String? {
    if (instant == null) return null
    return instant.atZone(MADRID_ZONE).format(SPANISH_DATE_FORMATTER)
}

fun fromSpanishDate(spanishDate: String?): Instant? {
    if (spanishDate == null) return null
    return try {
        val localDateTime = LocalDateTime.parse(spanishDate, SPANISH_DATE_FORMATTER)
        localDateTime.atZone(MADRID_ZONE).toInstant()
    } catch (e: Exception) {
        null
    }
}
