package com.rnett.krosstalk.client

import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.client.plugin.ClientHandler
import com.rnett.krosstalk.client.plugin.ClientScope

/**
 * The interface for a krosstalk client.  Have your Krosstalk object implement this to be a client.
 */
@OptIn(KrosstalkPluginApi::class)
public interface KrosstalkClient<C : ClientScope<*>> {
    @OptIn(KrosstalkPluginApi::class)
    public val client: ClientHandler<C>

    public val serverUrl: String
}