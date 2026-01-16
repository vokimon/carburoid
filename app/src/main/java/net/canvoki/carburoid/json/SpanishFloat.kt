package net.canvoki.carburoid.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

fun toSpanishFloat(value: Double?): String? = value?.toString()?.replace(".", ",")

fun fromSpanishFloat(value: String?): Double? = value?.replace(',', '.')?.toDoubleOrNull()

fun preprocessSpanishNumbers(json: String): String = Regex("\"([+-]?\\d+),(\\d+)\"").replace(json, "\"$1.$2\"")

fun postprocessSpanishNumbers(json: String): String = Regex("\"([+-]?\\d+)[.](\\d+)\"").replace(json, "\"$1,$2\"")

class SpanishFloatTypeAdapter : TypeAdapter<Double?>() {
    override fun read(reader: JsonReader): Double? =
        try {
            val s = reader.nextString()
            fromSpanishFloat(s)
        } catch (e: Exception) {
            reader.skipValue()
            null
        }

    override fun write(
        writer: JsonWriter,
        value: Double?,
    ) {
        if (value != null) {
            writer.value(toSpanishFloat(value))
        } else {
            writer.nullValue()
        }
    }
}
