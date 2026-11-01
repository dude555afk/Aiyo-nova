package com.beradeep.aiyo.data.remote

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import io.ktor.client.plugins.api.*
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
                        // Strip the SDK's empty Bearer header at the send stage,
                        // after the SDK auth plugin has prepared the request.
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
