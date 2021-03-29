package com.rnett.krosstalk

import kotlin.reflect.KClass

@OptIn(InternalKrosstalkApi::class)
@PublishedApi
@Suppress("unused")
internal inline fun <K, reified V> Map<K, *>.getValueAsOrError(
    methodName: String,
    key: K,
    default: () -> V?,
    nullError: String,
    typeError: String,
): V {
    val raw = this[key] ?: default() ?: throw KrosstalkException.MissingArgument(methodName, nullError)
    if (raw is V)
        return raw
    else throw KrosstalkException.WrongArgumentType(methodName,
        typeError.replace("\$type", raw::class.toString()).replace("\$required", V::class.toString()))
}

@PublishedApi
internal inline fun String.replace(target: String, value: () -> String) = if (target in this) replace(target, value()) else this

@OptIn(InternalKrosstalkApi::class)
@PublishedApi
@Suppress("unused")
internal inline fun handleException(
    t: Throwable,
    includeStackTrace: Boolean,
    printStackTrace: Boolean,
    handle: List<Triple<KClass<out Throwable>, Int, String>>,
): KrosstalkResult<Nothing> {
    var result: KrosstalkResult<Nothing>? = null

    handle.forEach { (cls, responseCode, message) ->
        println("Message template: $message")
        println("Exception message: ${t.message}")
        if (cls.isInstance(t)) {
            result = KrosstalkResult.HttpError(
                responseCode,
                message
                    .replace(exceptionMessage) { t.message ?: "N/A" }
                    .replace(exceptionStacktrace) { t.stackTraceToString() }
                    .replace(exception) { t.toString() }
            )
        }
    }

    return result ?: run {
        if (printStackTrace)
            t.printStackTrace()

        KrosstalkResult.ServerException(t, includeStackTrace)
    }

}