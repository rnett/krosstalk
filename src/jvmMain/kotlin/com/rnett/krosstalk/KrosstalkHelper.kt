package com.rnett.krosstalk

import kotlinx.serialization.cbor.Cbor
import kotlin.reflect.full.callSuspendBy

suspend fun <K> K.handle(data: ByteArray): ByteArray where K : Krosstalk<*>, K : KrosstalkServer<*> {
    val call = Cbor.load(KrosstalkCall.serializer(), data)
    val method = methods[call.function] ?: error("No method found for ${call.function}")
    val params = call.parameters.mapValues {
        val serializer = method.paramSerializers[it.key]
                ?: error("No serializer found for ${it.key}")
        serializer.deserialize(it.value)
    }.mapKeys { (key, _) ->
        method.method.parameters.singleOrNull { it.name == key } ?: error("Unknown parameter $key")
    }
    val result = method.method.callSuspendBy(params)
    val resultSerializer = method.resultSerializer as Serializer<Any?>
    return resultSerializer.serialize(result)
}