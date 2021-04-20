package com.rnett.krosstalk

import com.rnett.krosstalk.annotations.RespondWithHeaders

/**
 * An class containing a returned value, and associated headers.
 *
 * If used with [@RespondWithHeaders][RespondWithHeaders], [headers] will be the headers to send with the response (if on the server)
 * or the headers received with the response (if on the client).
 *
 * @see RespondWithHeaders
 */
public data class WithHeaders<out T>(val value: T, val headers: Headers = emptyMap())