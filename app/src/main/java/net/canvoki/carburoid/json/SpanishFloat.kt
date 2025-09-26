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
    override fun write(out: JsonWriter, value: Double?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(toSpanishFloat(value))
        }
    }

    override fun read(`in`: JsonReader): Double? {
        val raw = `in`.nextString()
        return fromSpanishFloat(raw)
    }
}
