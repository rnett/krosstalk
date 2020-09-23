package com.rnett.krosstalk

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface ClientScope<in D>
interface ServerScope


fun <C : ClientScope<D>, D, K> K.scope(scope: C) where K : Krosstalk, K : KrosstalkClient<in C> =
    ScopeAdder { ClientScopeHolder(this as Krosstalk, scope, it) }

fun <S : ServerScope, K> K.scope(server: S) where K : Krosstalk, K : KrosstalkServer<in S> =
    ScopeAdder { ServerScopeHolder(server, it) }

fun <K, S : ServerScope> K.neededServerScopes(method: MethodDefinition<*>) where K : Krosstalk, K : KrosstalkServer<S> =
    (method.requiredScopes + method.optionalScopes)
        .map {
            _scopes.getOrElse(it) { error("Unknown scope $it") } as? ServerScopeHolder<*>
                ?: error("$it wasn't a server scope")
        }
        .map { it.server }
        .map { it as? S ?: error("$it was not of correct scope type") }


sealed class ScopeHolder(val name: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScopeHolder) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

// I want equals() to be reference equality
class ServerScopeHolder<S : ServerScope>(val server: S, name: String) : ScopeHolder(name)

data class ActiveScope<D, out C : ClientScope<D>>(val scope: C, val data: D)

// I want equals() to be reference equality
@OptIn(ExperimentalStdlibApi::class)
class ClientScopeHolder<C : ClientScope<D>, D> @PublishedApi internal constructor(
    val krosstalk: Krosstalk,
    val scope: C,
    name: String
) : ScopeHolder(name) {

    inline fun <T> open(scope: D, block: () -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        open(scope)
        val result = block()
        close()
        return result
    }

    fun open(data: D) {
        krosstalk.activeScopes[this] = ActiveScope(scope, data)
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

    inline operator fun <T> invoke(data: D, block: () -> T): T = open(data, block)
//    inline operator fun <T> invoke(scope: C, block: () -> T): T = open(scope, block)
}


class ScopeAdder<S : ScopeHolder>(val scope: (String) -> S) {
    operator fun provideDelegate(thisRef: Krosstalk, prop: KProperty<*>): ReadOnlyProperty<Krosstalk, S> {
        val holder = scope(prop.name)
        thisRef._scopes[prop.name] = holder
        return object : ReadOnlyProperty<Krosstalk, S> {
            override inline operator fun getValue(thisRef: Krosstalk, prop: KProperty<*>) = holder
        }
    }
}
