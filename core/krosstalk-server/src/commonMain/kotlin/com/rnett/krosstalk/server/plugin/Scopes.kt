package com.rnett.krosstalk.server.plugin

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.server.KrosstalkServer
import kotlin.jvm.JvmName


@KrosstalkPluginApi
public val <S : ServerScope<*>, K> K.serverScopes: List<S> where K : Krosstalk, K : KrosstalkServer<S>
    get() = scopes.map {
        @Suppress("UNCHECKED_CAST")
        (it as? ServerScope<*> ?: error("Somehow had a client scope on the server side."))
                as? S ?: error("Scope $it is not of correct server scope type for krosstalk $this")
    }

@KrosstalkPluginApi
@JvmName("serverScopesAsType")
public fun <S : ServerScope<*>, K> K.scopesAsType(scopes: Iterable<Scope>): List<S> where K : Krosstalk, K : KrosstalkServer<S> =
    scopes.map {
        @Suppress("UNCHECKED_CAST")
        (it as? ServerScope<*> ?: error("Somehow had a client scope on the server side"))
                as? S ?: error("Scope $it was not of correct server scope type for krosstalk $this")
    }