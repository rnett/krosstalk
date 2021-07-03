package com.rnett.krosstalk

import com.rnett.krosstalk.client.plugin.ClientScope
import com.rnett.krosstalk.server.plugin.ServerScope

/**
 * Since scopes are declared as objects, all scope classes should be open to allow for use.
 *
 * While delegation is possible, it is easy to delegate from an interface (i.e. [ServerScope]) that doesn't define any methods.
 * Thus, it is discouraged in favor of implementation.
 */
public interface Scope {
    public val canBeOptional: Boolean get() = true
}

//TODO make value classes
public sealed class ScopeInstance<T : Scope> {
    public abstract val scope: T

    @OptIn(KrosstalkPluginApi::class)
    @InternalKrosstalkApi
    @Deprecated("Do not use, this is not reliable")
    public class Server<T : ServerScope<S>, S>(@property:InternalKrosstalkApi public val _data: S, override val scope: T) : ScopeInstance<T>()

    @OptIn(KrosstalkPluginApi::class)
    @InternalKrosstalkApi
    @Deprecated("Do not use, this is not reliable")
    public class Client<T : ClientScope<C>, C>(@property:InternalKrosstalkApi public val clientData: C, override val scope: T) : ScopeInstance<T>()
}
