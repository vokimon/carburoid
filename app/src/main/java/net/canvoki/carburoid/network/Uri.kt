package net.canvoki.carburoid.network

import java.net.URI
import java.net.URLDecoder

class Uri private constructor(private val uri: URI, private val queryMap: Map<String, String>) {
    val host: String? get() = uri.host
    val path: String? get() = uri.path
    val fragment: String? get() = uri.fragment

    fun getQueryParameter(name: String): String? = queryMap[name]

    companion object {
        fun parse(uriString: String): Uri? =
            try {
                val uri = URI(uriString)
                val queryMap = uri.query?.let { parseQuery(it) } ?: emptyMap()
                Uri(uri, queryMap)
            } catch (e: Exception) {
                null
            }

        private fun parseQuery(query: String): Map<String, String> =
            query.split("&").associate { param ->
                val parts = param.split("=", limit = 2)
                val key = URLDecoder.decode(parts[0], "UTF-8")
                val value = if (parts.size == 2) URLDecoder.decode(parts[1], "UTF-8") else ""
                key to value
            }
    }
}
