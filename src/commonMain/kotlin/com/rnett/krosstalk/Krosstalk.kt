package com.rnett.krosstalk

import kotlin.reflect.KCallable

data class MethodDefinition<T>(
    val method: KCallable<T>,
    val requiredScopes: List<ScopeOf<*, *>>,
    val paramSerializers: Map<String, Serializer<*>>,
    val resultSerializer: Serializer<T>
)

data class MethodSerializers<T>(
    val paramSerializers: Map<String, Serializer<*>>,
    val resultSerializer: Serializer<T>
)
class Scope<out C1: C, out S1: ServerScope, C: ClientScope> internal constructor(val server: S1, val krosstalk: Krosstalk<*, C, *>)
typealias ScopeOf<C, S> = Scope<C, S, C>

class ClientScopeHolder<C1: C, C: ClientScope> internal constructor(val krosstalk: Krosstalk<*, C, *>){
    operator fun <S1: ServerScope> plus(server: S1) = Scope<C1, S1, C>(server, krosstalk)
}

abstract class KrosstalkBase<D, C: ClientScope, S: ServerScope> internal constructor(){
    abstract val serialization: SerializationHandler<D>
    abstract val client: ClientHandler<C>
    abstract val server: ServerHandler<S>
    open val endpointName: String = "krosstalk"

    internal val methods = mutableMapOf<String, MethodDefinition<*>>()
    fun addMethod(key: String, method: KCallable<*>, extraData: D, vararg requiredScopes: ScopeOf<C, S>){
        if(key in methods)
            error("Already registered method with name $key")


        val serializers = serialization.getSerializers(method, extraData)
        methods[key] = MethodDefinition(method, requiredScopes.toList(), serializers.paramSerializers, serializers.resultSerializer)
    }
}

expect abstract class Krosstalk<D, C: ClientScope, S: ServerScope>() : KrosstalkBase<D, C, S>{
    fun <C1: C, S1: S> scope(server: S1): Scope<C1, S1, C> // = Scope<C1, S1, C, S>(server, this)
    fun <C1: C> clientScope(): ClientScopeHolder<C1, C> // = ClientScopeHolder<C1, C, S>(this)
}

fun <C: ClientScope, S: ServerScope> Krosstalk<Unit, C, S>.addMethod(key: String, method: KCallable<*>, vararg requiredScopes: ScopeOf<C, S>) = addMethod(key, method, Unit, *requiredScopes)