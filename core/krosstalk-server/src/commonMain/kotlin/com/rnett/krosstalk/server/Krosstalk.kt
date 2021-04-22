package com.rnett.krosstalk.server

import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.server.plugin.ServerHandler
import com.rnett.krosstalk.server.plugin.ServerScope

/**
 * The interface for a krosstalk server.  Have your Krosstalk object implement this to be a server.
 */
@OptIn(KrosstalkPluginApi::class)
public interface KrosstalkServer<S : ServerScope<*>> {
    @OptIn(KrosstalkPluginApi::class)
    public val server: ServerHandler<S>
}