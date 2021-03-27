package com.rnett.krosstalk.server

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ScopeInstance
import kotlin.jvm.JvmName

@OptIn(InternalKrosstalkApi::class)
@Suppress("UNCHECKED_CAST")
val <T : ServerScope<S>, S> ScopeInstance<T>.value
    get() =
        if (this !is ScopeInstance.Server<*, *>)
            error("Somehow had a client instance of a server scope.  This should be impossible.")
        else
            _data as S


@OptIn(InternalKrosstalkApi::class)
operator fun <T : ServerScope<S>, S> T.invoke(serverData: S): ScopeInstance<T> = ScopeInstance.Server(serverData, this)

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
