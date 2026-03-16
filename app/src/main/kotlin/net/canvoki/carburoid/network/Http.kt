package net.canvoki.carburoid.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import net.canvoki.carburoid.BuildConfig
import net.canvoki.shared.log

object Http {
    val client: HttpClient =
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 5)
                exponentialDelay()
            }
            install(UserAgent) {
                val appName = "Carburoid"
                val appVersion = BuildConfig.VERSION_NAME
                val projectUrl = "https://github.com/canvoki/carburoid"
                agent = "$appName/$appVersion ($projectUrl)"
                log(agent)
            }
        }
}
