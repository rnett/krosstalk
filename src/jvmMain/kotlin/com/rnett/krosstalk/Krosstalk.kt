package com.rnett.krosstalk

import kotlinx.serialization.cbor.Cbor
import kotlin.reflect.full.callSuspendBy



actual abstract class Krosstalk<D, C : ClientScope, S : ServerScope> : KrosstalkBase<D, C, S>() {
    actual fun <C1 : C, S1 : S> scope(server: S1): Scope<C1, S1, C> = Scope(server, this)

    actual fun <C1 : C> clientScope(): ClientScopeHolder<C1, C> = ClientScopeHolder(this)

    suspend fun handle(data: ByteArray): ByteArray {
        val call = Cbor.load(KrosstalkCall.serializer(), data)
        val method = methods[call.function] ?: error("No method found for ${call.function}")
        val params = call.parameters.mapValues {
            val serializer = method.paramSerializers[it.key] ?: error("No serializer found for ${it.key}")
            serializer.deserialize(it.value)
        }.mapKeys {  (key, _) ->
            method.method.parameters.singleOrNull { it.name == key } ?: error("Unknown parameter $key")
        }
        val result =  method.method.callSuspendBy(params)
        val resultSerializer = method.resultSerializer as Serializer<Any?>
        return resultSerializer.serialize(result)
    }
}