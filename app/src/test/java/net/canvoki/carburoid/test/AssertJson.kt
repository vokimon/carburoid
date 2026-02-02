package net.canvoki.carburoid.test

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals

fun canonicalizeJson(json: String): String {
    val result = Json.parseToJsonElement(json).toString()
    return result
}

fun assertJsonEqual(
    expected: String,
    result: String,
) {
    assertEquals(canonicalizeJson(expected), canonicalizeJson(result))
}
