package com.rnett.krosstalk

//TODO catch/convert to http status code
/**
 * An argument that should have been passed was not.
 */
@OptIn(InternalKrosstalkApi::class)
class MissingArgumentException @InternalKrosstalkApi constructor(val methodName: String, val subMessage: String) : KrosstalkException(
    "Method $methodName: $subMessage")

//TODO catch/convert to http status code

/**
 * An argument was passed as the wrong type.
 */
@OptIn(InternalKrosstalkApi::class)
class WrongArgumentTypeException @InternalKrosstalkApi constructor(val methodName: String, val subMessage: String) : KrosstalkException(
    "Method $methodName: $subMessage")

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
            throw MissingArgumentException(methodName, missingError)
    }

    if (raw is V) {
        return raw
    } else {
        throw WrongArgumentTypeException(methodName,
            typeError.replace("\$type", raw!!::class.toString()).replace("\$required", V::class.toString()))
    }
}