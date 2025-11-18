package net.canvoki.carburoid.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

class SaleTypeAdapter : TypeAdapter<Boolean>() {
    override fun write(
        out: JsonWriter,
        value: Boolean?,
    ) {
        out.value(
            when (value) {
                true -> "P"
                false -> "R"
                else -> ""
            },
        )
    }

    override fun read(`in`: JsonReader): Boolean? {
        if (`in`.peek() == JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        return when (`in`.nextString()) {
            "P" -> true
            "R" -> false
            else -> null
        }
    }
}
