package com.rnett.krosstalk.server

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.exception
import com.rnett.krosstalk.exceptionMessage
import com.rnett.krosstalk.exceptionStacktrace
import kotlin.reflect.KClass


@PublishedApi
internal inline fun String.replace(target: String, value: () -> String) = if (target in this) replace(target, value()) else this

@OptIn(InternalKrosstalkApi::class, KrosstalkPluginApi::class)
@PublishedApi
@Suppress("unused")
internal inline fun handleException(
    t: Throwable,
    includeStackTrace: Boolean,
    printStackTrace: Boolean,
    server: KrosstalkServer<*>,
    handle: List<Triple<KClass<out Throwable>, Int, String>>,
): KrosstalkResult<Nothing> {
    var result: KrosstalkResult<Nothing>? = null

    handle.forEach { (cls, responseCode, message) ->
        if (cls.isInstance(t)) {
            result = KrosstalkResult.HttpError(
                responseCode,
                server.server.getStatusCodeName(responseCode),
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