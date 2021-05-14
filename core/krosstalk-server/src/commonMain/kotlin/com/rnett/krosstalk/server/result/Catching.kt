package com.rnett.krosstalk.server.result

import com.rnett.krosstalk.result.KrosstalkResult
import kotlin.reflect.KClass

/**
 * Catch an exception by type.  **Must be called where the exception/KrosstalkResult is created**, type information is lost during serialization.
 */
public inline fun <R, T : R, reified E : Throwable> KrosstalkResult<T>.catch(klass: KClass<E>, value: (E) -> KrosstalkResult<R>): KrosstalkResult<R> =
    when (this) {
        is KrosstalkResult.ServerException -> if (this.throwable is E) value(this.throwable as E) else this
        is KrosstalkResult.SuccessOrHttpError -> this
    }


/**
 * Catch an exception by type.  **Must be called where the exception/KrosstalkResult is created**, type information is lost during serialization.
 */
public inline fun <R, T : R, reified E : Throwable> KrosstalkResult<T>.catchAsSuccess(klass: KClass<E>, value: (E) -> R): KrosstalkResult<R> =
    when (this) {
        is KrosstalkResult.ServerException -> if (this.throwable is E) KrosstalkResult.Success(value(this.throwable as E)) else this
        is KrosstalkResult.SuccessOrHttpError -> this
    }


/**
 * Catch an exception by type.  **Must be called where the exception/KrosstalkResult is created**, type information is lost during serialization.
 */
public inline fun <R, T : R, reified E : Throwable> KrosstalkResult<T>.catchAsHttpError(
    klass: KClass<E>,
    statusCode: Int,
    message: (E) -> String? = { it.message }
): KrosstalkResult<R> =
    when (this) {
        is KrosstalkResult.ServerException -> if (this.throwable is E) KrosstalkResult.HttpError(statusCode, message(this.throwable as E)) else this
        is KrosstalkResult.SuccessOrHttpError -> this
    }

//TODO this results in client/server behavior differences
///**
// * If the result is a [KrosstalkResult.ServerException] that matches [filter], if it's originating [Throwable] is available, throws it, otherwise throws via [KrosstalkResult.ServerException.throwFailureException].
// */
//public inline fun <T> KrosstalkResult<T>.rethrowServerExceptions(filter: (KrosstalkResult.ServerException) -> Boolean = { true }): KrosstalkResult<T> =
//    when (this) {
//        is KrosstalkResult.ServerException -> if (filter(this)) {
//            if (this.throwable != null)
//                throw this.throwable!!
//            else
//                this.throwFailureException()
//        } else this
//        else -> this
//    }
//
///**
// * If the result is a [KrosstalkResult.ServerException], if it's originating [Throwable] is available and is one of [classes], throws it.
// */
//public inline fun <T> KrosstalkResult<T>.rethrowServerExceptions(vararg classes: KClass<out Throwable>): KrosstalkResult<T> =
//    when (this) {
//        is KrosstalkResult.ServerException -> if (this.throwable != null && classes.any { it.isInstance(this.throwable) }) {
//            throw this.throwable!!
//        } else this
//        else -> this
//    }