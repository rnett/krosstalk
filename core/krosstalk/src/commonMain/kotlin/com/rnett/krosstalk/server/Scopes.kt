package com.rnett.krosstalk.server

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KrosstalkException
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ScopeInstance


//TODO require KrosstalkPluginApi for implementing
/**
 * Since scopes are declared as objects, all scope classes should be open to allow for use.
 *
 * While delegation is possible, it is easy to delegate from an interface (i.e. [ServerScope]) that doesn't define any methods.
 * Thus, it is discouraged in favor of implementation.
 */
@KrosstalkPluginApi
interface ServerScope<S> : Scope


/**
 * Some required scopes are missing from a Krosstalk call.
 */
@OptIn(InternalKrosstalkApi::class)
class MissingScopeException internal constructor(val scope: Scope, val methodName: String? = null) : KrosstalkException(
    "Missing required scope $scope${methodName?.let { " for method $it" }.orEmpty()}."
)

@KrosstalkPluginApi
interface WantedScopes {
    operator fun <T : ServerScope<D>, D> get(scope: T): D
    fun <T : ServerScope<D>, D> getOptional(scope: T): D?
    fun <T : ServerScope<D>, D> getOrElse(scope: T, default: D): D

    fun toImmutable(): ImmutableWantedScopes
    fun toMap(): Map<ServerScope<*>, Any?>
}

@KrosstalkPluginApi
class ImmutableWantedScopes internal constructor(values: Map<ServerScope<*>, Any?>) : WantedScopes {
    constructor() : this(emptyMap())

    private val values: Map<ServerScope<*>, Any?> = values.toMap()

    @OptIn(InternalKrosstalkApi::class)
    @PublishedApi
    internal fun <T : ServerScope<D>, D> getRequiredInstance(scope: T, methodName: String): ScopeInstance<T> =
        if (scope in values) ScopeInstance.Server(values[scope] as D, scope) else throw MissingScopeException(scope, methodName)

    @OptIn(InternalKrosstalkApi::class)
    @PublishedApi
    internal fun <T : ServerScope<D>, D> getOptionalInstance(scope: T): ScopeInstance<T>? =
        getOptional(scope)?.let { ScopeInstance.Server(it, scope) }

    override operator fun <T : ServerScope<D>, D> get(scope: T): D =
        if (scope in values) values[scope] as D else throw MissingScopeException(scope)

    override fun <T : ServerScope<D>, D> getOptional(scope: T): D? = values[scope] as D?

    override fun <T : ServerScope<D>, D> getOrElse(scope: T, default: D): D =
        if (scope in values) values[scope] as D else default

    override fun toMap() = values.toMap()
    override fun toImmutable(): ImmutableWantedScopes = this
}

@KrosstalkPluginApi
class MutableWantedScopes : WantedScopes {
    private val values = mutableMapOf<ServerScope<*>, Any?>()
    override operator fun <T : ServerScope<D>, D> get(scope: T): D =
        if (scope in values) values[scope] as D else throw MissingScopeException(scope)

    override fun <T : ServerScope<D>, D> getOptional(scope: T): D? = values[scope] as D?

    override fun <T : ServerScope<D>, D> getOrElse(scope: T, default: D): D =
        if (scope in values) values[scope] as D else default

    operator fun <T : ServerScope<D>, D> set(scope: T, value: D) {
        values[scope] = value
    }

    override fun toMap() = values.toMap()
    override fun toImmutable() = ImmutableWantedScopes(values.toMap())
}
