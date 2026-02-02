package net.canvoki.carburoid.test

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals

private fun JsonElement.canonicalize(): JsonElement =
    when (this) {
        is JsonObject -> JsonObject(
            entries
                .sortedBy { it.key }
                .associate { it.key to it.value.canonicalize() }
        )
        is JsonArray -> JsonArray(map { it.canonicalize() })
        else -> this
    }

private val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

private fun canonicalizeJson(jsonStr: String): String {
    val result = json.parseToJsonElement(jsonStr).canonicalize().toString()
    return result
}

fun assertJsonEqual(
    expected: String,
    result: String,
) {
    assertEquals(canonicalizeJson(expected), canonicalizeJson(result))
}
