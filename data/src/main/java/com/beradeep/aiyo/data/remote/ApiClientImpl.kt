package com.beradeep.aiyo.data.remote

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import kotlin.time.Duration.Companion.seconds

class ApiClientImpl : DataApiClient {
    init { load() }

    @Volatile
    override var openAI: OpenAI? = null

    override fun load() {
        synchronized(this) {
            // Kilo.ai anonymous gateway - no API key required
            openAI =
                OpenAI(
                    token = "",
                    host = OpenAIHost(DataApiClient.BASE_URL),
                    timeout = Timeout(connect = 30.seconds, socket = 120.seconds)
                )
        }
    }
}