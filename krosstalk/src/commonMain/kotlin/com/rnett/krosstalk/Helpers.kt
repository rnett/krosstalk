package com.rnett.krosstalk

@PublishedApi
@Suppress("unused")
internal inline fun <K, reified V> Map<K, *>.getValueAsOrError(key: K, default: () -> V?, nullError: String, typeError: String): V {
    val raw = this[key] ?: default() ?: error(nullError)
    if (raw is V)
        return raw
    else error(typeError.replace("\$type", raw::class.toString()).replace("\$required", V::class.toString()))
}