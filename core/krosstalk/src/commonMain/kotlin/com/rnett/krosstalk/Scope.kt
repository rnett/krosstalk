package com.rnett.krosstalk

import com.rnett.krosstalk.client.ClientScope
import com.rnett.krosstalk.server.ServerScope

/**
 * Since scopes are declared as objects, all scope classes should be open to allow for use.
 *
 * While delegation is possible, it is easy to delegate from an interface (i.e. [ServerScope]) that doesn't define any methods.
 * Thus, it is discouraged in favor of implementation.
 */
interface Scope {
    val canBeOptional get() = true
}


sealed class ScopeInstance<T : Scope> {
    abstract val scope: T

    @OptIn(KrosstalkPluginApi::class)
    @InternalKrosstalkApi
    class Server<T : ServerScope<S>, S>(@InternalKrosstalkApi val _data: S, override val scope: T) : ScopeInstance<T>()

    @OptIn(KrosstalkPluginApi::class)
    @InternalKrosstalkApi
    class Client<T : ClientScope<C>, C>(@InternalKrosstalkApi val clientData: C, override val scope: T) : ScopeInstance<T>()
}
