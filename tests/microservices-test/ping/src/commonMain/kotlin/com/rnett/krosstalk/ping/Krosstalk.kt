package com.rnett.krosstalk.ping

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.annotations.KrosstalkMethod
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler

internal expect object PingKrosstalk : Krosstalk {
    override val serialization: KotlinxBinarySerializationHandler
}

@KrosstalkMethod(com.rnett.krosstalk.ping.PingKrosstalk::class)
expect suspend fun ping(n: Int, call: Boolean = true): Boolean