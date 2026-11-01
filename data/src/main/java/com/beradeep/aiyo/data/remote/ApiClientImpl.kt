package com.beradeep.aiyo.data.remote

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import kotlin.time.Duration.Companion.seconds

class ApiClientImpl : DataApiClient {
    init { load() }

    @Volatile
    override var openAI: OpenAI? = null

    override fun load() {
        synchronized(this) {
            openAI =
                OpenAI(
                    token = "",
                    host = OpenAIHost(DataApiClient.BASE_URL),
                    timeout = Timeout(connect = 30.seconds, socket = 120.seconds),
                    httpClientConfig = {
                        // The OpenAI SDK installs Bearer auth even for an empty token.
                        // Strip it after the Auth plugin so Kilo receives a genuinely
                        // anonymous request. Kilo only permits this for free models.
                        install(
                            createClientPlugin("KiloAnonymousAuthorizationStripper") {
                                on(Send) { request ->
                                    request.headers.remove(HttpHeaders.Authorization)
                                    proceed(request)
                                }
                            }
                        )
                    }
                )
        }
    }
}
