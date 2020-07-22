package com.rnett.krosstalk

import kotlinx.serialization.cbor.Cbor
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.instanceParameter

//TODO need to use compiler plugin to generate part of this for backends without callSuspendBy
suspend fun <K> K.handle(data: ByteArray): ByteArray where K : Krosstalk, K : KrosstalkServer<*> {
    val call = Cbor.load(KrosstalkCall.serializer(), data)
    val method = methods[call.function] ?: error("No method found for ${call.function}")
    val params = call.parameters.mapValues {
        val serializer = method.serializers.paramSerializers[it.key]
                ?: error("No serializer found for ${it.key}")
        serializer.deserialize(it.value)
    }.mapKeys { (key, _) ->
        method.method.parameters.singleOrNull { it.name == key } ?: error("Unknown parameter $key")
    }.toMutableMap()

    if (method.method.instanceParameter != null) {
        params[method.method.instanceParameter!!] = (
                (method.serializers.instanceReceiverSerializer
                        ?: error("No instance receiver serializer found, but argument passed"))
                        as Serializer<Any?>).deserialize(call.instanceReceiver!!)
    }

    if (method.method.extensionReceiverParameter != null) {
        params[method.method.extensionReceiverParameter!!] = (
                (method.serializers.extensionReceiverSerializer
                        ?: error("No extension receiver serializer found, but argument passed"))
                        as Serializer<Any?>).deserialize(call.extensionReceiver!!)
    }

    val result = method.method.callSuspendBy(params)
    val resultSerializer = method.serializers.resultSerializer as Serializer<Any?>
    return resultSerializer.serialize(result)
}