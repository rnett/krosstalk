package com.rnett.krosstalk.server

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.server.plugin.ServerScope

@OptIn(InternalKrosstalkApi::class, KrosstalkPluginApi::class)
@Suppress("UNCHECKED_CAST", "DEPRECATED")
public val <T : ServerScope<S>, S> ScopeInstance<T>.value: S
    get() =
        if (this !is ScopeInstance.Server<*, *>)
            error("Somehow had a client instance of a server scope.  This should be impossible.")
        else
            _data as S


@OptIn(InternalKrosstalkApi::class, KrosstalkPluginApi::class)
@Suppress("DEPRECATED")
public operator fun <T : ServerScope<S>, S> T.invoke(serverData: S): ScopeInstance<T> = ScopeInstance.Server(serverData, this)
