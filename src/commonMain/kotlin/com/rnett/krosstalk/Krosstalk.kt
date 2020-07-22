package com.rnett.krosstalk

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty

data class MethodDefinition<T>(
        val method: KCallable<T>,
        val requiredScopes: List<String>,
        val paramSerializers: Map<String, Serializer<*>>,
        val resultSerializer: Serializer<T>
)

data class MethodSerializers<T>(
        val paramSerializers: Map<String, Serializer<*>>,
        val resultSerializer: Serializer<T>
)

sealed class ScopeHolder
class ServerScopeHolder<S : ServerScope>(val server: S) : ScopeHolder()

@OptIn(ExperimentalStdlibApi::class)
class ClientScopeHolder<C : ClientScope> internal constructor(val krosstalk: Krosstalk<*>) : ScopeHolder() {

    inline fun <T> open(scope: C, block: () -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        krosstalk.activeScopes.add(scope)
        val result = block()
        krosstalk.activeScopes.removeLast()
        return result
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
    operator fun provideDelegate(thisRef: Krosstalk<*>, prop: KProperty<*>): ReadOnlyProperty<Krosstalk<*>, S> {
        thisRef.scopes[prop.name] = scope
        return object : ReadOnlyProperty<Krosstalk<*>, S> {
            override inline operator fun getValue(thisRef: Krosstalk<*>, prop: KProperty<*>) = scope
        }
    }
}

/*
    TODO re-architect to not lock in client-server, use interface style scope defs, not need common for everything, etc.  Different client/server & serialization for each module, same function registration
 */
abstract class Krosstalk<D> internal constructor() {
    abstract val serialization: SerializationHandler<D>
    open val endpointName: String = "krosstalk"

    @PublishedApi
    internal val methods = mutableMapOf<String, MethodDefinition<*>>()

    fun addMethod(key: String, method: KCallable<*>, extraData: D, vararg requiredScopes: String) {
        if (key in methods)
            error("Already registered method with name $key")


        val serializers = serialization.getSerializers(method, extraData)
        methods[key] = MethodDefinition(method, requiredScopes.toList(), serializers.paramSerializers, serializers.resultSerializer)
    }

    @PublishedApi
    internal val activeScopes = mutableListOf<ClientScope>()

    //TODO scope registering should be handled by compiler plugin
    @PublishedApi
    internal val scopes = mutableMapOf<String, ScopeHolder>()
}

interface KrosstalkClient<C : ClientScope> {
    val client: ClientHandler<C>
    fun <C1 : C> scope() = ScopeAdder(ClientScopeHolder<C1>(this as? Krosstalk<*>
            ?: error("Can't implement KrosstalkClient without also extending Krosstalk")))
}

interface KrosstalkServer<S : ServerScope> {
    val server: ServerHandler<S>
    fun <S1 : S> scope(server: S1) = ScopeAdder(ServerScopeHolder(server))
}

fun <C : ClientScope, S : ServerScope> Krosstalk<Unit>.addMethod(key: String, method: KCallable<*>, vararg requiredScopes: String) = addMethod(key, method, Unit, *requiredScopes)

fun <K, S : ServerScope> K.requiredServerScopes(method: MethodDefinition<*>) where K : Krosstalk<*>, K : KrosstalkServer<S> =
        method.requiredScopes
                .map {
                    scopes.getOrElse(it) { error("Unknown scope $it") } as? ServerScopeHolder<*>
                            ?: error("$it wasn't a server scope")
                }
                .map { it.server }
                .map { it as? S ?: error("$it was not of correct scope type") }