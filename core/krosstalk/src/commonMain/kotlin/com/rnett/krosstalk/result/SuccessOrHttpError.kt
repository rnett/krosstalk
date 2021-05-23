package com.rnett.krosstalk.result

import kotlin.contracts.contract

/**
 * Transform the success value.
 */
public inline fun <T, R> KrosstalkResult.SuccessOrHttpError<T>.map(transform: (T) -> R): KrosstalkResult.SuccessOrHttpError<R> = when (this) {
    is KrosstalkResult.Success -> KrosstalkResult.Success(transform(value))
    is KrosstalkResult.HttpError -> this
}

/**
 * Get a value depending on the type of result.
 */
public inline fun <T, R> KrosstalkResult.SuccessOrHttpError<T>.foldHttpError(
    onSuccess: (KrosstalkResult.Success<T>) -> R,
    onHttpError: (KrosstalkResult.HttpError) -> R
): R = when (this) {
    is KrosstalkResult.Success -> onSuccess(this)
    is KrosstalkResult.HttpError -> onHttpError(this)
}

/**
 * If this is a [KrosstalkResult.HttpError], throw [KrosstalkHttpError].
 */
public fun <T> KrosstalkResult.SuccessOrHttpError<T>.throwOnHttpError(): T {
    contract {
        returns() implies (this@throwOnHttpError !is KrosstalkResult.HttpError)
        returns() implies (this@throwOnHttpError is KrosstalkResult.Success)
    }

    if (this is KrosstalkResult.HttpError) {
        throwFailureException()
    }
    return this.valueOrThrow
}

/**
 * Gets the value on success, or the result of [onFailure] otherwise.
 *
 * Alias for `fold({ it }, onHttpError)`.
 */
public inline fun <R, T : R> KrosstalkResult.SuccessOrHttpError<T>.getOrElseHttpError(onHttpError: (KrosstalkResult.HttpError) -> R): R =
    foldHttpError({ it.value }, onHttpError)

/**
 * Recover from all http errors.  Note that exceptions will not be caught, so [KrosstalkResult.HttpError.throwFailureException] can be used
 * to throw on unhandled http errors.
 */
public inline fun <R, T : R> KrosstalkResult.SuccessOrHttpError<T>.recoverHttpErrors(onHttpError: (KrosstalkResult.HttpError) -> R): R =
    when (this) {
        is KrosstalkResult.Success -> this.value
        is KrosstalkResult.HttpError -> onHttpError(this)
    }

/**
 * Handle http errors matching [filter].
 */
public inline fun <R, T : R> KrosstalkResult.SuccessOrHttpError<T>.handleHttpError(
    filter: (KrosstalkResult.HttpError) -> Boolean,
    onHttpError: (KrosstalkResult.HttpError) -> R
): KrosstalkResult.SuccessOrHttpError<R> =
    when (this) {
        is KrosstalkResult.Success -> this
        is KrosstalkResult.HttpError -> if (filter(this)) KrosstalkResult.Success(onHttpError(this)) else this
    }

/**
 * Handle http errors with status codes of [statusCode].
 */
public inline fun <R, T : R> KrosstalkResult.SuccessOrHttpError<T>.handleHttpError(
    statusCode: Int,
    onHttpError: (KrosstalkResult.HttpError) -> R
): KrosstalkResult.SuccessOrHttpError<R> =
    handleHttpError({ it.statusCode == statusCode }, onHttpError)