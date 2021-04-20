package com.rnett.krosstalk

import com.rnett.krosstalk.client.ClientScope
import com.rnett.krosstalk.server.ServerScope

/**
 * Since scopes are declared as objects, all scope classes should be open to allow for use.
 *
 * While delegation is possible, it is easy to delegate from an interface (i.e. [ServerScope]) that doesn't define any methods.
 * Thus, it is discouraged in favor of implementation.
 */
public interface Scope {
    public val canBeOptional: Boolean get() = true
}


public sealed class ScopeInstance<T : Scope> {
    public abstract val scope: T

    @OptIn(KrosstalkPluginApi::class)
    @InternalKrosstalkApi
    public class Server<T : ServerScope<S>, S>(@InternalKrosstalkApi public val _data: S, override val scope: T) : ScopeInstance<T>()

    @OptIn(KrosstalkPluginApi::class)
    @InternalKrosstalkApi
    public class Client<T : ClientScope<C>, C>(@InternalKrosstalkApi public val clientData: C, override val scope: T) : ScopeInstance<T>()
}
