package com.rnett.krosstalk.native_test

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.client.krosstalkCall
import com.rnett.krosstalk.client.plugin.ClientHandler
import com.rnett.krosstalk.annotations.KrosstalkMethod
import com.rnett.krosstalk.client.KrosstalkClient
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import kotlinx.serialization.cbor.Cbor

object TestKrosstalk: Krosstalk(), KrosstalkClient<Nothing> {
    override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val serverUrl: String = "http://localhost:8080"

    override val client get() = error("No client, this Krosstalk is just for plugin tests")
}

@KrosstalkMethod(TestKrosstalk::class)
suspend fun test(): Unit = krosstalkCall()