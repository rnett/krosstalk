package com.rnett.krosstalk

@OptIn(InternalKrosstalkApi::class)
@PublishedApi
@Suppress("unused")
internal inline fun <K, reified V> Map<K, *>.getValueAsOrError(
    methodName: String,
    key: K,
    optional: Boolean,
    missingError: String,
    typeError: String,
    default: () -> V = { error("Impossible, default was not specified for non-optional parameter.") },
): V {
    val raw = if (key in this) {
        this.getValue(key)
    } else {
        if (optional)
            default()
        else
            throw KrosstalkException.MissingArgument(methodName, missingError)
    }

    if (raw is V) {
        return raw
    } else {
        throw KrosstalkException.WrongArgumentType(methodName,
            typeError.replace("\$type", raw!!::class.toString()).replace("\$required", V::class.toString()))
    }
}