package com.rnett.krosstalk.result

import kotlin.contracts.contract

/**
 * Transform the success value.
 */
public inline fun <T, R> KrosstalkResult.SuccessOrServerException<T>.map(transform: (T) -> R): KrosstalkResult.SuccessOrServerException<R> =
    when (this) {
        is KrosstalkResult.Success -> KrosstalkResult.Success(transform(value))
        is KrosstalkResult.ServerException -> this
    }

/**
 * Get a value depending on the type of result.
 */
public inline fun <T, R> KrosstalkResult.SuccessOrServerException<T>.foldServerException(
    onSuccess: (KrosstalkResult.Success<T>) -> R,
    onServerException: (KrosstalkResult.ServerException) -> R
): R = when (this) {
    is KrosstalkResult.Success -> onSuccess(this)
    is KrosstalkResult.ServerException -> onServerException(this)
}

/**
 * If this is a [KrosstalkResult.ServerException], throw [KrosstalkServerException].
 */
public fun <T> KrosstalkResult.SuccessOrServerException<T>.throwOnServerException(): T {
    contract {
        returns() implies (this@throwOnServerException !is KrosstalkResult.ServerException)
        returns() implies (this@throwOnServerException is KrosstalkResult.Success)
    }

    if (this is KrosstalkResult.ServerException) {
        throwFailureException()
    }
    return this.valueOrThrow
}

/**
 * Gets the value on success, or the result of [onFailure] otherwise.
 *
 * Alias for `fold({ it }, onServerException)`.
 */
public inline fun <R, T : R> KrosstalkResult.SuccessOrServerException<T>.getOrElseServerException(onServerException: (KrosstalkResult.ServerException) -> R): R =
    foldServerException({ it.value }, onServerException)

/**
 * Recover from all server exceptions.  Note that exceptions will not be caught, so [KrosstalkResult.ServerException.throwFailureException] can be used
 * to throw on unhandled server exceptions.
 */
public inline fun <R, T : R> KrosstalkResult.SuccessOrServerException<T>.recoverServerExceptions(onServerException: (KrosstalkResult.ServerException) -> R): R =
    when (this) {
        is KrosstalkResult.Success -> this.value
        is KrosstalkResult.ServerException -> onServerException(this)
    }

/**
 * Handle server exceptions matching [filter].
 */
public inline fun <R, T : R> KrosstalkResult.SuccessOrServerException<T>.handleServerException(
    filter: (KrosstalkResult.ServerException) -> Boolean,
    onServerException: (KrosstalkResult.ServerException) -> R
): KrosstalkResult.SuccessOrServerException<R> =
    when (this) {
        is KrosstalkResult.Success -> this
        is KrosstalkResult.ServerException -> if (filter(this)) KrosstalkResult.Success(onServerException(this)) else this
    }

/**
 * Handle server exceptions with a [KrosstalkResult.ServerException.className] of [className].
 */
public inline fun <R, T : R> KrosstalkResult.SuccessOrServerException<T>.handleServerException(
    className: String,
    onServerException: (KrosstalkResult.ServerException) -> R
): KrosstalkResult.SuccessOrServerException<R> = handleServerException({ it.className == className }, onServerException)