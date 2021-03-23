package com.rnett.krosstalk

import kotlin.jvm.JvmName

interface Scope {
    val canBeOptional get() = true
}

interface ClientScope<in D> : Scope
interface ServerScope<S> : Scope

sealed class ScopeInstance<T : Scope> {
    abstract val scope: T

    @PublishedApi
    internal class Server<T : ServerScope<S>, S>(internal val _data: S, override val scope: T) : ScopeInstance<T>()

    //TODO mark clientData somehow to ensure it's not used outside of client impls
    class Client<T : ClientScope<C>, C>(val clientData: C, override val scope: T) : ScopeInstance<T>()
}

data class AppliedClientScope<T : ClientScope<D>, D>(val scope: T, val data: D)

@PublishedApi
internal fun <T : ClientScope<D>, D> ScopeInstance<T>?.toAppliedScope(): AppliedClientScope<T, D>? = this?.run {
    if (this is ScopeInstance.Client<*, *>) {
        return AppliedClientScope(scope, clientData as D)
    }
    error("Can't convert a server scope to an applied client scope")
}

interface WantedScopes {
    operator fun <T : ServerScope<D>, D> get(scope: T): D
    fun <T : ServerScope<D>, D> getOptional(scope: T): D?
    fun <T : ServerScope<D>, D> getOrElse(scope: T, default: D): D

    fun toImmutable(): ImmutableWantedScopes
    fun toMap(): Map<ServerScope<*>, Any?>
}

//TODO make internal?  Can't quite.  Should have distinct end-user and client/server/serialization implementer packages, end-user doesn't need stuff like this
class ImmutableWantedScopes internal constructor(private val values: Map<ServerScope<*>, Any?>) : WantedScopes {
    constructor() : this(emptyMap())

    override operator fun <T : ServerScope<D>, D> get(scope: T): D =
        if (scope in values) values[scope] as D else error("No value for scope $scope")

    override fun <T : ServerScope<D>, D> getOptional(scope: T): D? = values[scope] as D?

    override fun <T : ServerScope<D>, D> getOrElse(scope: T, default: D): D =
        if (scope in values) values[scope] as D else default

    override fun toMap() = values.toMap()
    override fun toImmutable(): ImmutableWantedScopes = ImmutableWantedScopes(values.toMap())
}

class MutableWantedScopes : WantedScopes {
    private val values = mutableMapOf<ServerScope<*>, Any?>()
    override operator fun <T : ServerScope<D>, D> get(scope: T): D =
        if (scope in values) values[scope] as D else error("No value for scope $scope")

    override fun <T : ServerScope<D>, D> getOptional(scope: T): D? = values[scope] as D?

    override fun <T : ServerScope<D>, D> getOrElse(scope: T, default: D): D =
        if (scope in values) values[scope] as D else default

    operator fun <T : ServerScope<D>, D> set(scope: T, value: D) {
        values[scope] = value
    }

    override fun toMap() = values.toMap()
    override fun toImmutable() = ImmutableWantedScopes(values.toMap())
}

@Suppress("UNCHECKED_CAST")
val <T : ServerScope<S>, S> ScopeInstance<T>.value
    get() =
        if (this !is ScopeInstance.Server<*, *>)
            error("Somehow had a client instance of a server scope.  This should be impossible.")
        else
            _data as S

operator fun <T : ClientScope<C>, C> T.invoke(clientData: C): ScopeInstance.Client<T, *> = ScopeInstance.Client(clientData, this)

operator fun <T : ServerScope<S>, S> T.invoke(serverData: S): ScopeInstance<T> = ScopeInstance.Server(serverData, this)

//TODO one for Scope for multiplatform use?  Doesn't really work since types aren't exposed and don't match.  Would be good to have for unit scopes maybe?


val <C : ClientScope<*>, K> K.clientScopes: List<C> where K : Krosstalk, K : KrosstalkClient<C>
    get() = scopes.map {
        (it as? ClientScope<*> ?: error("Somehow had a server scope on the client side."))
                as? C ?: error("Scope $it is not of correct client scope type for krosstalk $this")
    }

@JvmName("clientScopesAsType")
fun <C : ClientScope<*>, K> K.scopesAsType(scopes: Iterable<Scope>): List<C> where K : Krosstalk, K : KrosstalkClient<C> =
    scopes.map {
        (it as? ClientScope<*> ?: error("Somehow had a server scope on the client side"))
                as? C ?: error("Scope $it was not of correct client scope type for krosstalk $this")
    }

val <S : ServerScope<*>, K> K.serverScopes: List<S> where K : Krosstalk, K : KrosstalkServer<S>
    get() = scopes.map {
        (it as? ServerScope<*> ?: error("Somehow had a client scope on the server side."))
                as? S ?: error("Scope $it is not of correct server scope type for krosstalk $this")
    }

@JvmName("serverScopesAsType")
fun <S : ServerScope<*>, K> K.scopesAsType(scopes: Iterable<Scope>): List<S> where K : Krosstalk, K : KrosstalkServer<S> =
    scopes.map {
        (it as? ServerScope<*> ?: error("Somehow had a client scope on the server side"))
                as? S ?: error("Scope $it was not of correct server scope type for krosstalk $this")
    }

