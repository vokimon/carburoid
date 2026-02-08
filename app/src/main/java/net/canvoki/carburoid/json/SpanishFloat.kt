package net.canvoki.carburoid.json

fun toSpanishFloat(value: Double?): String? = value?.toString()?.replace(".", ",")

fun fromSpanishFloat(value: String?): Double? = value?.replace(',', '.')?.toDoubleOrNull()

fun preprocessSpanishNumbers(json: String): String = Regex("\"([+-]?\\d+),(\\d+)\"").replace(json, "\"$1.$2\"")

fun postprocessSpanishNumbers(json: String): String = Regex("\"([+-]?\\d+)[.](\\d+)\"").replace(json, "\"$1,$2\"")
