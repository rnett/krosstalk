package com.rnett.krosstalk.server

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KrosstalkException
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.KrosstalkResult


@PublishedApi
internal inline fun String.replace(target: String, value: () -> String) = if (target in this) replace(target, value()) else this

@OptIn(InternalKrosstalkApi::class, KrosstalkPluginApi::class)
@PublishedApi
@Suppress("unused")
internal inline fun <T> wrapResult(
    result: KrosstalkResult<T>,
    includeStackTrace: Boolean,
    server: KrosstalkServer<*>,
): KrosstalkResult<T> = when (result) {
    is KrosstalkResult.Success -> result
    is KrosstalkResult.ServerException -> result.withIncludeStackTrace(includeStackTrace)
    is KrosstalkResult.HttpError -> result.copy(statusCodeName = server.server.getStatusCodeName(result.statusCode))
}

@OptIn(InternalKrosstalkApi::class)
@PublishedApi
internal inline fun serverExceptionOrThrowKrosstalk(
    t: Throwable,
    includeStackTrace: Boolean,
): KrosstalkResult.ServerException {
    if (t is KrosstalkException)
        throw t
    else
        return KrosstalkResult.ServerException(t, includeStackTrace)
}