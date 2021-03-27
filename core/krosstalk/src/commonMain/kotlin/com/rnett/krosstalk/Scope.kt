package com.rnett.krosstalk

import com.rnett.krosstalk.client.ClientScope
import com.rnett.krosstalk.server.ServerScope

interface Scope {
    val canBeOptional get() = true
}


sealed class ScopeInstance<T : Scope> {
    abstract val scope: T

    @InternalKrosstalkApi
    class Server<T : ServerScope<S>, S>(@InternalKrosstalkApi val _data: S, override val scope: T) : ScopeInstance<T>()

    class Client<T : ClientScope<C>, C>(@InternalKrosstalkApi val clientData: C, override val scope: T) : ScopeInstance<T>()
}
