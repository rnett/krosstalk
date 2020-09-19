package com.rnett.krosstalk

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

data class MethodDefinition<T>(
//        val method: KCallable<T>,
    val requiredScopes: List<String>,
    val serializers: MethodSerializers,
    val call: suspend (Map<String, *>) -> T
) {
    val hasInstanceParameter = serializers.instanceReceiverSerializer != null
    val hasExtensionParameter = serializers.extensionReceiverSerializer != null
}

sealed class ScopeHolder
class ServerScopeHolder<S : ServerScope>(val server: S) : ScopeHolder()

@OptIn(ExperimentalStdlibApi::class)
class ClientScopeHolder<C : ClientScope> internal constructor(val krosstalk: Krosstalk) : ScopeHolder() {

    inline fun <T> open(scope: C, block: () -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        open(scope)
        val result = block()
        close()
        return result
    }

    fun open(scope: C) {
        krosstalk.activeScopes[this] = scope
    }

    val isOpen get() = this in krosstalk.activeScopes
    fun close() {
        krosstalk.activeScopes.remove(this)
    }

//    inline fun open(scope: C, block: () -> Unit){
//        contract {
//            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
//        }
//        krosstalk.activeScopes.add(scope)
//        block()
//        krosstalk.activeScopes.removeLast()
//    }

    inline operator fun <T> invoke(scope: C, block: () -> T): T = open(scope, block)
//    inline operator fun <T> invoke(scope: C, block: () -> T): T = open(scope, block)
}


class ScopeAdder<S : ScopeHolder>(val scope: S) {
    operator fun provideDelegate(thisRef: Krosstalk, prop: KProperty<*>): ReadOnlyProperty<Krosstalk, S> {
        thisRef.scopes[prop.name] = scope
        return object : ReadOnlyProperty<Krosstalk, S> {
            override inline operator fun getValue(thisRef: Krosstalk, prop: KProperty<*>) = scope
        }
    }
}


abstract class Krosstalk {
    abstract val serialization: SerializationHandler
    open val endpointName: String = "krosstalk"

    @PublishedApi
    internal val _methods = mutableMapOf<String, MethodDefinition<*>>()

    val methods: Map<String, MethodDefinition<*>> = _methods

    //TODO make internal
//    @PublishedApi
    fun <T> addMethod(
        key: String/*, method: KCallable<*>*/,
        types: MethodTypes,
        vararg requiredScopes: String,
        call: suspend (Map<String, *>) -> T
    ) {
        if (key in methods)
            error("Already registered method with name $key")


        val serializers = serialization.getAndCheckSerializers(types)
        _methods[key] = MethodDefinition(/*method, */requiredScopes.toList(), serializers, call)
    }

    @PublishedApi
    internal val activeScopes = mutableMapOf<ClientScopeHolder<*>, ClientScope>()

    fun closeAllScopes() {
        activeScopes.clear()
    }

    //TODO scope registering should be handled by compiler plugin
    @PublishedApi
    internal val scopes = mutableMapOf<String, ScopeHolder>()
}

interface KrosstalkClient<C : ClientScope> {
    val client: ClientHandler<C>
    fun <C1 : C> scope() = ScopeAdder(ClientScopeHolder<C1>(this as? Krosstalk
            ?: error("Can't implement KrosstalkClient without also extending Krosstalk")))
}

interface KrosstalkServer<S : ServerScope> {
    val server: ServerHandler<S>
    fun <S1 : S> scope(server: S1) = ScopeAdder(ServerScopeHolder(server))
}

fun <K, S : ServerScope> K.requiredServerScopes(method: MethodDefinition<*>) where K : Krosstalk, K : KrosstalkServer<S> =
        method.requiredScopes
                .map {
                    scopes.getOrElse(it) { error("Unknown scope $it") } as? ServerScopeHolder<*>
                            ?: error("$it wasn't a server scope")
                }
                .map { it.server }
                .map { it as? S ?: error("$it was not of correct scope type") }