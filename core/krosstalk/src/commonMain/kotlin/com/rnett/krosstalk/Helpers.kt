package com.rnett.krosstalk

@OptIn(InternalKrosstalkApi::class)
@PublishedApi
@Suppress("unused")
internal inline fun <K, reified V> Map<K, *>.getValueAsOrError(methodName: String, key: K, default: () -> V?, nullError: String, typeError: String): V {
    val raw = this[key] ?: default() ?: throw KrosstalkException.MissingArgument(methodName, nullError)
    if (raw is V)
        return raw
    else throw KrosstalkException.WrongArgumentType(methodName, typeError.replace("\$type", raw::class.toString()).replace("\$required", V::class.toString()))
}