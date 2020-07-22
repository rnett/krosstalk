package com.rnett.krosstalk

import kotlinx.serialization.cbor.Cbor

suspend inline fun <T, K, reified C : ClientScope> K.call(methodName: String, parameters: Map<String, *>): T where K : KrosstalkClient<C>, K : Krosstalk<*> {
    val method = methods[methodName] ?: error("Unknown method $methodName")
    val serializedParams = parameters.mapValues {
        val serializer = method.paramSerializers[it.key]
                ?: error("No serializer found for param ${it.key}")
        (serializer as Serializer<Any?>).serialize(it.value)
    }
    val data = Cbor.dump(KrosstalkCall.serializer(), KrosstalkCall(methodName, serializedParams))
    val result = client.sendKrosstalkRequest(methodName, data, activeScopes.map {
        it as? C ?: error("Scope $it was not of required type ${C::class}")
    })
    return method.resultSerializer.deserialize(result) as T
}