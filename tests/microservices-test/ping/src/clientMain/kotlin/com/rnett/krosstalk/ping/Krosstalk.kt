package com.rnett.krosstalk.ping

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.client.krosstalkCall
import com.rnett.krosstalk.ktor.client.KtorClient
import com.rnett.krosstalk.ktor.client.KtorKrosstalkClient
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import kotlinx.serialization.cbor.Cbor

internal actual object PingKrosstalk : Krosstalk(), KtorKrosstalkClient {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val client: KtorClient = KtorClient()
    override val serverUrl: String = "http://localhost:8083"
}

actual suspend fun ping(n: Int, call: Boolean): Boolean = krosstalkCall()