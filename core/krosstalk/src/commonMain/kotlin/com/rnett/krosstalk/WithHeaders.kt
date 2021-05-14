package com.rnett.krosstalk

import com.rnett.krosstalk.annotations.RespondWithHeaders
import kotlin.jvm.JvmName

//TODO make value class
/**
 * An class containing a returned value, and associated headers.
 *
 * If used with [@RespondWithHeaders][RespondWithHeaders], [headers] will be the headers to send with the response (if on the server)
 * or the headers received with the response (if on the client).
 *
 * @see RespondWithHeaders
 */
public data class WithHeaders<out T>(val value: T, val headers: Headers = headersOf()) {
    public constructor(value: T, vararg header: Pair<String, String>) : this(value, headersOf(*header))

    /**
     * Copy this [WithHeaders] and add some new headers.
     */
    public inline fun addHeaders(block: MutableHeaders.() -> Unit): WithHeaders<T> = copy(headers = headers.addHeaders(block))
}

@JvmName("WithHeadersList")
public fun <T> WithHeaders(value: T, vararg header: Pair<String, List<String>>): WithHeaders<T> = WithHeaders(value, headersOf(*header))

public inline fun <T> WithHeaders(value: T, headers: MutableHeaders.() -> Unit): WithHeaders<T> = WithHeaders(value, buildHeaders(headers))
public inline fun <T> T.withHeaders(headers: MutableHeaders.() -> Unit): WithHeaders<T> = WithHeaders(this, headers)