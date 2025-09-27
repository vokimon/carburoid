package net.canvoki.carburoid.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

fun toSpanishFloat(value: Double?): String? {
    return value?.toString()?.replace(".", ",")
}
fun fromSpanishFloat(value: String?): Double? {
    return value?.replace(',', '.')?.toDoubleOrNull()
}


class SpanishFloatTypeAdapter : TypeAdapter<Double?>() {
    override fun read(reader: JsonReader): Double? {
        return try {
            reader.nextDouble()
        } catch (e: Exception) {
            reader.skipValue()
            null
        }
    }

    override fun write(writer: JsonWriter, value: Double?) {
        if (value != null) {
            writer.value(toSpanishFloat(value))
        } else {
            writer.nullValue()
        }
    }
}
