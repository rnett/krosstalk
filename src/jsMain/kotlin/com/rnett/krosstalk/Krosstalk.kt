package com.rnett.krosstalk

import kotlinx.serialization.cbor.Cbor
import kotlin.contracts.contract

actual abstract class Krosstalk<D, C : ClientScope, S : ServerScope> : KrosstalkBase<D, C, S>(){
    internal val activeScopes = mutableListOf<C>()
    actual fun <C1 : C, S1 : S> scope(server: S1): Scope<C1, S1, C> = Scope(server, this)

    actual fun <C1 : C> clientScope(): ClientScopeHolder<C1, C> = ClientScopeHolder(this)

    suspend fun <T> call(methodName: String, parameters: Map<String, *>): T{
        val method = methods[methodName] ?: error("Unknown method $methodName")
        val serializedParams = parameters.mapValues {
            val serializer = method.paramSerializers[it.key] ?:  error("No serializer found for param ${it.key}")
            (serializer as Serializer<Any?>).serialize(it.value)
        }
        val data = Cbor.dump(KrosstalkCall.serializer(), KrosstalkCall(methodName, serializedParams))
        val result = client.sendKrosstalkRequest(methodName, data, activeScopes)
        return method.resultSerializer.deserialize(result) as T
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun <C: ClientScope, T> ScopeOf<C, *>.open(client: C, block: () -> T): T{
    contract {
        callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    krosstalk.activeScopes.add(client)
    val ret = block()
    krosstalk.activeScopes.removeLast()
    return ret
}

@OptIn(ExperimentalStdlibApi::class)
fun <C: ClientScope> ScopeOf<C, *>.open(client: C, block: () -> Unit){
    contract {
        callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    krosstalk.activeScopes.add(client)
    block()
    krosstalk.activeScopes.removeLast()
}

//operator fun <C: ClientScope, T> ScopeOf<C, *>.invoke(client: C, block: () -> T) = open(client, block)
//operator fun <C: ClientScope> ScopeOf<C, *>.invoke(client: C, block: () -> Unit) = open(client, block)

@OptIn(ExperimentalStdlibApi::class)
suspend fun <C: ClientScope, T> ScopeOf<C, *>.openSuspend(client: C, block: suspend () -> T): T{
    contract {
        callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    krosstalk.activeScopes.add(client)
    val ret = block()
    krosstalk.activeScopes.removeLast()
    return ret
}

@OptIn(ExperimentalStdlibApi::class)
suspend fun <C: ClientScope> ScopeOf<C, *>.openSuspend(client: C, block: suspend () -> Unit){
    contract {
        callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    krosstalk.activeScopes.add(client)
    block()
    krosstalk.activeScopes.removeLast()
}

suspend operator fun <C: ClientScope, T> ScopeOf<C, *>.invoke(client: C, block: suspend () -> T) = openSuspend(client, block)
suspend operator fun <C: ClientScope> ScopeOf<C, *>.invoke(client: C, block: suspend () -> Unit) = openSuspend(client, block)