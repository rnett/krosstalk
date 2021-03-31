package com.rnett.krosstalk.client

import com.rnett.krosstalk.KrosstalkPluginApi


/**
 * The interface for a krosstalk client.  Have your Krosstalk object implement this to be a client.
 */
@OptIn(KrosstalkPluginApi::class)
interface KrosstalkClient<C : ClientScope<*>> {
    @OptIn(KrosstalkPluginApi::class)
    val client: ClientHandler<C>
}