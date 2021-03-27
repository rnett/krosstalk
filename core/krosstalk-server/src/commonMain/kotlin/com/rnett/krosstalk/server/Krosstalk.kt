package com.rnett.krosstalk.server

import com.rnett.krosstalk.KrosstalkPluginApi

/**
 * The interface for a krosstalk server.  Have your Krosstalk object implement this to be a server.
 */
interface KrosstalkServer<S : ServerScope<*>> {
    @OptIn(KrosstalkPluginApi::class)
    val server: ServerHandler<S>
}