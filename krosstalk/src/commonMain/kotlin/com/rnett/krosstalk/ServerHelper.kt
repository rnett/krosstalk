package com.rnett.krosstalk

import kotlinx.serialization.cbor.Cbor

const val instanceParameter = "\$instance"
const val extensionParameter = "\$extension"

//TODO need to use compiler plugin to generate part of this for backends without callSuspendBy
suspend fun <K> K.handle(data: ByteArray): ByteArray where K : Krosstalk, K : KrosstalkServer<*> {
    val call = Cbor.decodeFromByteArray(KrosstalkCall.serializer(), data)
    val method = methods[call.function] ?: error("No method found for ${call.function}")

    val params = call.parameters.mapValues {
        val serializer = method.serializers.paramSerializers[it.key]
            ?: error("No serializer found for ${it.key}")
        serializer.deserialize(it.value)
    }.toMutableMap()

    if (method.hasInstanceParameter) {
        params[instanceParameter] = (
                (method.serializers.instanceReceiverSerializer
                    ?: error("No instance receiver serializer found, but argument required"))
                        as Serializer<Any?>).deserialize(
            call.instanceReceiver ?: error("No instance receiver argument passed, but it is required")
        )
    }



    if (method.hasExtensionParameter) {
        params[extensionParameter] = (
                (method.serializers.extensionReceiverSerializer
                    ?: error("No extension receiver serializer found, but argument required"))
                        as Serializer<Any?>).deserialize(
            call.extensionReceiver ?: error("No extension receiver argument passed, but it is required")
        )
    }

    val result = method.call(params)
    val resultSerializer = method.serializers.resultSerializer as Serializer<Any?>
    return resultSerializer.serialize(result)
}