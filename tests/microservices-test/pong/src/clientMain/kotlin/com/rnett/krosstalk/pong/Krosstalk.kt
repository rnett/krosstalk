package com.rnett.krosstalk.pong

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.client.krosstalkCall
import com.rnett.krosstalk.ktor.client.KtorClient
import com.rnett.krosstalk.ktor.client.KtorKrosstalkClient
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import io.ktor.client.HttpClient
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import kotlinx.serialization.cbor.Cbor

internal actual object PongKrosstalk : Krosstalk(), KtorKrosstalkClient {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val client: KtorClient = KtorClient(HttpClient() {
        Logging {
            level = LogLevel.ALL
        }
    })
    override val serverUrl: String = "http://localhost:8084"
}

actual suspend fun pong(n: Int, call: Boolean): Boolean = krosstalkCall()