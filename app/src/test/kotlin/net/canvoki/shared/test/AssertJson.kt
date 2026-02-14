package net.canvoki.shared.test

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

private fun JsonElement.sortKeys(): JsonElement =
    when (this) {
        is JsonObject ->
            JsonObject(
                entries
                    .sortedBy { it.key }
                    .associate { it.key to it.value.sortKeys() },
            )
        is JsonArray -> JsonArray(map { it.sortKeys() })
        else -> this
    }

private val json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        prettyPrint = true
        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
        prettyPrintIndent = "  "
    }

private fun canonicalizeJson(jsonStr: String): String {
    val element = json.parseToJsonElement(jsonStr).sortKeys()
    return json.encodeToString(JsonElement.serializer(), element)
}

fun assertJsonEqual(
    expected: String,
    result: String,
) {
    assertEquals(canonicalizeJson(expected), canonicalizeJson(result))
}
