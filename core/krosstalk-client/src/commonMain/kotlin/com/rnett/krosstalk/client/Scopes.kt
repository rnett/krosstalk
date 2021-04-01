package com.rnett.krosstalk.client

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ScopeInstance
import kotlin.jvm.JvmName

@KrosstalkPluginApi
data class AppliedClientScope<T : ClientScope<D>, D>(val scope: T, val data: D)

@OptIn(KrosstalkPluginApi::class, InternalKrosstalkApi::class)
@PublishedApi
internal fun <T : ClientScope<D>, D> ScopeInstance<T>?.toAppliedScope(): AppliedClientScope<T, D>? = this?.run {
    if (this is ScopeInstance.Client<*, *>) {
        return AppliedClientScope(scope, clientData as D)
    }
    error("Can't convert a server scope to an applied client scope")
}


@OptIn(KrosstalkPluginApi::class, InternalKrosstalkApi::class)
operator fun <T : ClientScope<C>, C> T.invoke(clientData: C): ScopeInstance<T> = ScopeInstance.Client(clientData, this)

@KrosstalkPluginApi
val <C : ClientScope<*>, K> K.clientScopes: List<C> where K : Krosstalk, K : KrosstalkClient<C>
    get() = scopes.map {
        (it as? ClientScope<*> ?: error("Somehow had a server scope on the client side."))
                as? C ?: error("Scope $it is not of correct client scope type for krosstalk $this")
    }

@KrosstalkPluginApi
@JvmName("clientScopesAsType")
fun <C : ClientScope<*>, K> K.scopesAsType(scopes: Iterable<Scope>): List<C> where K : Krosstalk, K : KrosstalkClient<C> =
    scopes.map {
        (it as? ClientScope<*> ?: error("Somehow had a server scope on the client side"))
                as? C ?: error("Scope $it was not of correct client scope type for krosstalk $this")
    }
