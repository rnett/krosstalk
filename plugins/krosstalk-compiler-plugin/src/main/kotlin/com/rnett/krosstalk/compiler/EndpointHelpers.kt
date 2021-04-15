package com.rnett.krosstalk.compiler

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.endpoint.Endpoint
import com.rnett.krosstalk.endpoint.EndpointPart


/**
 * Returns true if each param in [requiredParameters] ends up in the endpoint if it is non-null.
 */
@OptIn(InternalKrosstalkApi::class)
fun Endpoint.hasWhenNotNull(param: String): Boolean {
    resolveOptionals(setOf(param)).forEachPart(false) {
        if (it is EndpointPart.Parameter && it.param == param)
            return true
    }
    return false
}

fun Endpoint.referencedParametersWhenOptionalFalse(falseOptionals: Set<String>): Set<String> {
    val params = mutableSetOf<String>()
    forEachPart({ it !in falseOptionals }) {
        if (it is EndpointPart.Parameter)
            params += it.param
    }
    return params
}

fun Endpoint.usedOptionals(): Set<String> {
    val opts = mutableSetOf<String>()
    forEachPart {
        if (it is EndpointPart.Optional)
            opts += it.key
    }
    return opts
}

fun Endpoint.topLevelParameters(): Set<String> {
    val params = mutableSetOf<String>()
    forEachPart(false) {
        if (it is EndpointPart.Parameter)
            params += it.param
    }
    return params
}