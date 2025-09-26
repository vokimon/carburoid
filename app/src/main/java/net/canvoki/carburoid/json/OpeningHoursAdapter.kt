package net.canvoki.carburoid.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import net.canvoki.carburoid.model.OpeningHours

class OpeningHoursAdapter : TypeAdapter<OpeningHours>() {
    override fun write(out: JsonWriter, value: OpeningHours?) {
        out.value(value?.toString() ?: "")
    }

    override fun read(`in`: JsonReader): OpeningHours? {
        if (`in`.peek() == JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        return OpeningHours.parse(`in`.nextString())
    }
}
