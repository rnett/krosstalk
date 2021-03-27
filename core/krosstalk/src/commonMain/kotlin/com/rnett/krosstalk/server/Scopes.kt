package com.rnett.krosstalk.server

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KrosstalkException
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.Scope


//TODO require KrosstalkPluginApi for implementing
interface ServerScope<S> : Scope


/**
 * Some required scopes are missing from a Krosstalk call.
 */
@OptIn(InternalKrosstalkApi::class)
class MissingScopeException internal constructor(val scope: Scope) : KrosstalkException(
    "Missing required scope $scope."
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
