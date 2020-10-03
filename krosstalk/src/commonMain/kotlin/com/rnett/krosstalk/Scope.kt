package com.rnett.krosstalk

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface ClientScope<in D>
interface ServerScope


/**
 * Client scopes.
 */
inline val <K, C : ClientScope<*>> K.scopes: Map<String, ClientScopeHolder<C, *>> where K : Krosstalk, K : KrosstalkClient<C>
    @JvmName("clientScopes")
    get() = _scopes.mapValues { it as ClientScopeHolder<C, *> }

/**
 * Server scopes.
 */
inline val <K, S : ServerScope> K.scopes: Map<String, ServerScopeHolder<S>> where K : Krosstalk, K : KrosstalkServer<S>
    @JvmName("serverScopes")
    get() = _scopes.mapValues { it as ServerScopeHolder<S> }

fun <K, C : ClientScope<D>, D> K.getScope(scope: String): C where K : Krosstalk, K : KrosstalkClient<C> =
    (
            _scopes.getOrElse(scope) { error("Unknown scope $scope") } as? ClientScopeHolder<*, *>
                ?: error("$scope wasn't a client scope")
            ).scope as? C ?: error("$scope was not of correct scope type")

fun <K, S : ServerScope> K.getScope(scope: String): S where K : Krosstalk, K : KrosstalkServer<S> =
    (
            _scopes.getOrElse(scope) { error("Unknown scope $scope") } as? ServerScopeHolder<*>
                ?: error("$scope wasn't a server scope")
            ).scope as? S ?: error("$scope was not of correct scope type")

/**
 * Define a client scope.  Used as a delegate, i.e. `val auth by scope(ClientAuth(...))`.
 */
fun <C : ClientScope<D>, D, K> K.scope(scope: C): PropertyDelegateProvider<Krosstalk, ReadOnlyProperty<Krosstalk, ClientScopeHolder<C, D>>> where K : Krosstalk, K : KrosstalkClient<in C> =
    ScopeAdder { ClientScopeHolder(this as Krosstalk, scope, it) }

/**
 * Define a server scope.  Used as a delegate, i.e. `val auth by scope(ServerAuth(...))`.
 */
fun <S : ServerScope, K> K.scope(server: S): PropertyDelegateProvider<Krosstalk, ReadOnlyProperty<Krosstalk, ServerScopeHolder<S>>> where K : Krosstalk, K : KrosstalkServer<in S> =
    ScopeAdder { ServerScopeHolder(server, it) }

/**
 * A needed server scope, and whether it is optional
 */
data class NeededScope<S : ServerScope>(val scope: S, val optional: Boolean)

//TODO differentiate between optional and required.  Return Pair<ServerScope, Bool> or similar?
/**
 * Get the needed server scopes for a given definition.
 */
fun <K, S : ServerScope> K.neededServerScopes(method: MethodDefinition<*>): List<NeededScope<S>> where K : Krosstalk, K : KrosstalkServer<S> =
    method.requiredScopes.map {
        NeededScope(
            getScope(it),
            false
        )
    } + method.optionalScopes.map { NeededScope(getScope(it), true) }


/**
 * A defined scope.
 */
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
/**
 * A defined server scope.  Holds the scope configuration [scope] and the name of the scope [name].
 */
class ServerScopeHolder<S : ServerScope> internal constructor(val scope: S, name: String) : ScopeHolder(name)

/**
 * An activated client scope, and the data used to activate it.
 */
data class ActiveScope<D, out C : ClientScope<D>> internal constructor(val scope: C, val data: D)

// I want equals() to be reference equality
/**
 * A defined client scope.  Holds the scope configuration [scope] and the name of the scope [name].
 * Can be opened with data of the right type ([D]).
 */
class ClientScopeHolder<C : ClientScope<D>, D> @PublishedApi internal constructor(
        private val krosstalk: Krosstalk,
        val scope: C,
        name: String
) : ScopeHolder(name) {

    /**
     * Open this scope with [data] for [block].
     */
    inline fun <T> open(scope: D, block: () -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        open(scope)
        val result = block()
        close()
        return result
    }

    /**
     * Open this scope with [data], until closed.
     */
    fun open(data: D) {
        krosstalk.activeScopes[this] = ActiveScope(scope, data)
    }

    /**
     * Whether this scope is open.
     */
    val isOpen get() = this in krosstalk.activeScopes

    /**
     * Close this scope if it is open.
     */
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

    /**
     * Open this scope with [data] for [block].
     */
    inline operator fun <T> invoke(data: D, block: () -> T): T = open(data, block)
//    inline operator fun <T> invoke(scope: C, block: () -> T): T = open(scope, block)
}

/**
 * Open this scope until closed.
 */
fun <C : ClientScope<Unit>> ClientScopeHolder<C, Unit>.open() = open(Unit)

/**
 * Open this scope for [block].
 */
inline fun <C : ClientScope<Unit>, T> ClientScopeHolder<C, Unit>.open(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return open(Unit, block)
}

/**
 * Open this scope for [block].
 */
inline operator fun <C : ClientScope<Unit>, T> ClientScopeHolder<C, Unit>.invoke(block: () -> T): T {
    return open(block)
}

//TODO Why am I using PropertyDelegateProvider instead of just ReadOnlyProperty?  Name?
/**
 * Scope delegate.
 */
internal class ScopeAdder<S : ScopeHolder> internal constructor(val scope: (String) -> S) : PropertyDelegateProvider<Krosstalk, ReadOnlyProperty<Krosstalk, S>> {
    override operator fun provideDelegate(thisRef: Krosstalk, prop: KProperty<*>): ReadOnlyProperty<Krosstalk, S> {
        val holder = scope(prop.name)
        thisRef._scopes[prop.name] = holder
        return ReadOnlyProperty { _, _ -> holder }
    }
}
