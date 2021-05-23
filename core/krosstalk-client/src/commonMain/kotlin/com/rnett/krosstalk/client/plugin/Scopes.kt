package com.rnett.krosstalk.client.plugin

import com.rnett.krosstalk.*
import com.rnett.krosstalk.client.KrosstalkClient
import kotlin.jvm.JvmName


@KrosstalkPluginApi
public data class AppliedClientScope<T : ClientScope<D>, D>(val scope: T, val data: D)

@OptIn(KrosstalkPluginApi::class, InternalKrosstalkApi::class)
@PublishedApi
internal fun <T : ClientScope<D>, D> ScopeInstance<T>?.toAppliedScope(): AppliedClientScope<T, D>? = this?.run {
    @Suppress("DEPRECATED")
    if (this is ScopeInstance.Client<*, *>) {
        @Suppress("UNCHECKED_CAST")
        return AppliedClientScope(scope, clientData as D)
    }
    error("Can't convert a server scope to an applied client scope")
}

@KrosstalkPluginApi
public val <C : ClientScope<*>, K> K.clientScopes: List<C> where K : Krosstalk, K : KrosstalkClient<C>
    get() = scopes.map {
        @Suppress("UNCHECKED_CAST")
        (it as? ClientScope<*> ?: error("Somehow had a server scope on the client side."))
                as? C ?: error("Scope $it is not of correct client scope type for krosstalk $this")
    }

@KrosstalkPluginApi
@JvmName("clientScopesAsType")
public fun <C : ClientScope<*>, K> K.scopesAsType(scopes: Iterable<Scope>): List<C> where K : Krosstalk, K : KrosstalkClient<C> =
    scopes.map {
        @Suppress("UNCHECKED_CAST")
        (it as? ClientScope<*> ?: error("Somehow had a server scope on the client side"))
                as? C ?: error("Scope $it was not of correct client scope type for krosstalk $this")
    }