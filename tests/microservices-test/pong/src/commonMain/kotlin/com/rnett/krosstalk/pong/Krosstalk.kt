package com.rnett.krosstalk.pong

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.annotations.KrosstalkMethod
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler

internal expect object PongKrosstalk : Krosstalk {
    override val serialization: KotlinxBinarySerializationHandler
}

@KrosstalkMethod(PongKrosstalk::class)
expect suspend fun pong(n: Int, call: Boolean = true): Boolean