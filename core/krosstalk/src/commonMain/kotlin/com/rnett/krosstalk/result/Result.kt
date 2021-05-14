package com.rnett.krosstalk.result

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KrosstalkException
import com.rnett.krosstalk.annotations.ExplicitResult
import com.rnett.krosstalk.httpStatusCodes
import com.rnett.krosstalk.result.KrosstalkResult.Failure
import com.rnett.krosstalk.result.KrosstalkResult.SuccessOrHttpError
import com.rnett.krosstalk.result.KrosstalkResult.SuccessOrServerException
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass

/**
 * An exception representing a non-success HTTP error code from a Krosstalk method.
 */
@OptIn(InternalKrosstalkApi::class)
public class KrosstalkHttpError(public val httpError: KrosstalkResult.HttpError) :
    KrosstalkException(buildString {
        "KrosstalkResult is http error code ${httpError.statusCode}"
        if (httpError.statusCodeName != null)
            append(": ${httpError.statusCodeName}")

        if (httpError.message != null && !httpError.message.isBlank())
            append(", with message: ${httpError.message}")
    })

/**
 * Throw a [KrosstalkHttpError].
 */
public inline fun throwKrosstalkHttpError(statusCode: Int, message: String? = null): Nothing {
    throw KrosstalkHttpError(KrosstalkResult.HttpError(statusCode, message))
}

/**
 * An exception representing an exception on the server side (i.e. a HTTP 500 response, but with more information).
 */
@OptIn(InternalKrosstalkApi::class)
public class KrosstalkServerException(public val exception: KrosstalkResult.ServerException) :
    KrosstalkException("KrosstalkResult is exception $exception")

/**
 * An exception representing an exception on the server side (i.e. a HTTP 500 response, but with more information).
 */
@OptIn(InternalKrosstalkApi::class)
public class KrosstalkUncaughtServerException @InternalKrosstalkApi constructor(public val exception: KrosstalkResult.ServerException) :
    KrosstalkException("KrosstalkResult is exception $exception")

/**
 * Throw a [KrosstalkServerException].
 *
 * Note that [includeStackTrace] will be overridden by a `false` value specified in a method's configuration,
 * and so should almost always be `true`.
 * A `false` value here can't be overridden by a method, since the necessary information won't be present.
 */
@OptIn(InternalKrosstalkApi::class)
public inline fun throwKrosstalkServerException(throwable: Throwable, includeStackTrace: Boolean = true): Nothing {
    throw KrosstalkServerException(KrosstalkResult.ServerException(throwable, includeStackTrace))
}

/**
 * Runs [block] and wraps the result if it is a success.  If [block] throws,
 * it wraps the resulting exception in [KrosstalkResult.ServerException].
 *
 * If the caught exception is [KrosstalkServerException] or [KrosstalkHttpError],
 * they are unwrapped instead of wrapped (i.e. their contained [KrosstalkResult] is returned).
 *
 * Note that [includeStackTrace] will be overridden by a `false` value specified in a method's configuration,
 * and so should almost always be `true`.
 * A `false` value here can't be overridden by a method, since the necessary information won't be present.
 *
 * @see catchKrosstalkExceptions
 */
@OptIn(InternalKrosstalkApi::class)
public inline fun <T> runKrosstalkCatching(includeStackTrace: Boolean = true, block: () -> T): KrosstalkResult<T> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return try {
        KrosstalkResult.Success(block())
    } catch (e: KrosstalkServerException) {
        return e.exception
    } catch (e: KrosstalkHttpError) {
        return e.httpError
    } catch (t: Throwable) {
        KrosstalkResult.ServerException(t, includeStackTrace)
    }
}

/**
 * Catch and unwrap [KrosstalkServerException] and [KrosstalkHttpError], without converting other exceptions to [KrosstalkServerException].
 *
 * @see runKrosstalkCatching
 */
public inline fun <T> catchKrosstalkExceptions(block: () -> T): KrosstalkResult<T> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return try {
        KrosstalkResult.Success(block())
    } catch (e: KrosstalkServerException) {
        return e.exception
    } catch (e: KrosstalkHttpError) {
        return e.httpError
    }
}

/**
 * Convert a [Result] to a [KrosstalkResult], using [KrosstalkResult.ServerException] to represent a failure.
 *
 * If the caught exception is [KrosstalkServerException] or [KrosstalkHttpError],
 * they are unwrapped instead of wrapped (i.e. their contained [KrosstalkResult] is returned).
 *
 * Note that [includeStackTrace] will be overridden by a `false` value specified in a method's configuration,
 * and so should almost always be `true`.
 * A `false` value here can't be overridden by a method, since the necessary information won't be present.
 *
 * @see runKrosstalkCatching
 */
@OptIn(InternalKrosstalkApi::class)
public inline fun <T> Result<T>.toKrosstalkResult(includeStackTrace: Boolean = true): KrosstalkResult<T> {
    return if (this.isSuccess)
        KrosstalkResult.Success(this.getOrThrow())
    else
        when (val exception = this.exceptionOrNull()!!) {
            is KrosstalkHttpError -> exception.httpError
            is KrosstalkServerException -> exception.exception
            else -> KrosstalkResult.ServerException(exception, includeStackTrace)
        }
}


/**
 * The result of a krosstalk call.  Can be either an exception in the server method, a Http error, or success.
 *
 * Can be handled using a `when` block (including using [Failure], [SuccessOrServerException], and [SuccessOrHttpError], since everything is `sealed`),
 * or using the provided `recover*`, `handle*`, and `throw*` functions.
 * Will confine the type to [SuccessOrServerException] or [SuccessOrHttpError] when possible (i.e. after a `throws*` function).
 *
 * To use as a return type of a Krosstalk method, use with [@ExplicitResult][ExplicitResult].
 */
public sealed interface KrosstalkResult<out T> {

    /**
     * A failure case of [KrosstalkResult]
     */
    public sealed interface Failure : KrosstalkResult<Nothing> {
        public fun getFailureException(): KrosstalkException

        public fun throwFailureException(): Nothing {
            throw getFailureException()
        }
    }

    /**
     * A successful [KrosstalkResult].
     */
    public data class Success<out T>(val value: T) : KrosstalkResult<T>, SuccessOrHttpError<T>, SuccessOrServerException<T>

    /**
     * The HTTP error [KrosstalkResult], containing the HTTP status code and an optional message.
     */
    public data class HttpError(val statusCode: Int, val message: String? = null) : Failure, SuccessOrHttpError<Nothing> {
        override fun getFailureException(): KrosstalkHttpError = KrosstalkHttpError(this)

        /**
         * The name of [statusCode].  Generated from [https://developer.mozilla.org/en-US/docs/Web/HTTP/Status](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status).
         */
        @OptIn(InternalKrosstalkApi::class)
        val statusCodeName: String? by lazy { httpStatusCodes[statusCode] }

        override fun toString(): String {
            return "HttpError(statusCode=${
                buildString {
                    append(statusCode)
                    if (statusCodeName != null)
                        append(" $statusCodeName")
                }
            }, message=$message)"
        }
    }

    /**
     * The server exception [KrosstalkResult], containing the [Throwable]'s data.
     *
     * **Note that [className] will vary depending on where the exception was thrown from!**
     * If it was thrown on JVM or native, it will be the [KClass.qualifiedName], but on JS that is not available,
     * so it will be [KClass.simpleName].
     */
    @Serializable
    public data class ServerException internal constructor(
        val className: String?,
        val classSimpleName: String?,
        val message: String?,
        val cause: ServerException?,
        val suppressed: List<ServerException>,
        val asStringNoStacktrace: String,
        val asStringWithStacktrace: String?,
        @Transient
        @InternalKrosstalkApi val throwable: Throwable? = null,
    ) : Failure, SuccessOrServerException<Nothing> {

        public constructor(throwable: Throwable, includeStacktrace: Boolean = true) : this(
            getClassName(throwable::class),
            throwable::class.simpleName,
            throwable.message,
            throwable.cause?.let { ServerException(it, includeStacktrace) },
            throwable.suppressedExceptions.map { ServerException(it, includeStacktrace) },
            throwable.toString(),
            if (includeStacktrace) throwable.stackTraceToString() else null,
            throwable
        )

        public fun withIncludeStackTrace(includeStacktrace: Boolean): ServerException = copy(
            cause = cause?.withIncludeStackTrace(includeStacktrace),
            suppressed = suppressed.map { it.withIncludeStackTrace(includeStacktrace) },
            asStringWithStacktrace = if (includeStacktrace) asStringWithStacktrace else null
        )

        inline val asString: String get() = asStringWithStacktrace ?: asStringNoStacktrace

        override fun getFailureException(): KrosstalkServerException = KrosstalkServerException(this)
    }

    /**
     * A [KrosstalkResult] with no [ServerException]s
     */
    public sealed interface SuccessOrHttpError<out T> : KrosstalkResult<T>

    /**
     * A [KrosstalkResult] with no [HttpError]
     */
    public sealed interface SuccessOrServerException<out T> : KrosstalkResult<T>
}

/**
 * Convert to a [Result], using the [KrosstalkHttpError] or [KrosstalkServerException] exceptions.
 */
public inline fun <T> KrosstalkResult<T>.toResult(): Result<T> = fold({ Result.success(it.value) }, { Result.failure(it.getFailureException()) })

/**
 * Get the value if successful, else null
 */
public inline val <T> KrosstalkResult<T>.valueOrNull: T? get() = fold({ it.value }, { null })

/**
 * Get the value if successful, otherwise throw with [throwOnFailure]
 */
public inline val <T> KrosstalkResult<T>.valueOrThrow: T get() = fold({ it.value }, { it.throwFailureException() })

/**
 * Get the server exception if there is one, else null
 */
public inline val <T> KrosstalkResult<T>.serverExceptionOrNull: KrosstalkResult.ServerException? get() = if (this.isServerException()) this else null

/**
 * Get the http error if there is one, else null
 */
public inline val <T> KrosstalkResult<T>.httpErrorOrNull: KrosstalkResult.HttpError? get() = if (this.isHttpError()) this else null

/**
 * Get the failure if there is one, else null
 */
public inline val <T> KrosstalkResult<T>.failureOrNull: KrosstalkResult.Failure? get() = fold({ null }, { it })

/**
 * Whether the result is success.
 */
public inline fun <T> KrosstalkResult<T>.isSuccess(): Boolean {
    contract {
        returns(true) implies (this@isSuccess is KrosstalkResult.Success)
        returns(false) implies (this@isSuccess !is KrosstalkResult.Success)
    }
    return this is KrosstalkResult.Success
}

/**
 * Whether the result is a server exception.
 */
public inline fun <T> KrosstalkResult<T>.isServerException(): Boolean {
    contract {
        returns(true) implies (this@isServerException is KrosstalkResult.ServerException)
        returns(false) implies (this@isServerException !is KrosstalkResult.ServerException)
    }
    return this is KrosstalkResult.ServerException
}

/**
 * Whether the result is a http error.
 */
public inline fun <T> KrosstalkResult<T>.isHttpError(): Boolean {
    contract {
        returns(true) implies (this@isHttpError is KrosstalkResult.HttpError)
        returns(false) implies (this@isHttpError !is KrosstalkResult.HttpError)
    }
    return this is KrosstalkResult.HttpError
}

/**
 * Whether the result is not a success.
 */
public inline fun <T> KrosstalkResult<T>.isFailure(): Boolean {
    contract {
        returns(true) implies (this@isFailure is KrosstalkResult.Failure)
        returns(false) implies (this@isFailure !is KrosstalkResult.HttpError)
        returns(false) implies (this@isFailure !is KrosstalkResult.ServerException)
    }
    return this is KrosstalkResult.Failure
}

/**
 * Get a value depending on the type of result.
 */
public inline fun <T, R> KrosstalkResult<T>.fold(
    onSuccess: (KrosstalkResult.Success<T>) -> R,
    onHttpError: (KrosstalkResult.HttpError) -> R,
    onServerException: (KrosstalkResult.ServerException) -> R
): R = when (this) {
    is KrosstalkResult.Success -> onSuccess(this)
    is KrosstalkResult.HttpError -> onHttpError(this)
    is KrosstalkResult.ServerException -> onServerException(this)
}

/**
 * Get a value depending on the type of result.
 */
public inline fun <T, R> KrosstalkResult<T>.fold(onSuccess: (KrosstalkResult.Success<T>) -> R, onFailure: (KrosstalkResult.Failure) -> R): R =
    when (this) {
        is KrosstalkResult.Success -> onSuccess(this)
        is KrosstalkResult.Failure -> onFailure(this)
    }

/**
 * Transform the success value.
 */
public inline fun <T, R> KrosstalkResult<T>.map(transform: (T) -> R): KrosstalkResult<R> = when (this) {
    is KrosstalkResult.Success -> KrosstalkResult.Success(transform(value))
    is KrosstalkResult.ServerException -> this
    is KrosstalkResult.HttpError -> this
}

/**
 * If this is a [HttpError], throw [KrosstalkHttpError].  If this is a [ServerException], throw [KrosstalkServerException].
 */
public fun <T> KrosstalkResult<T>.throwOnFailure(): T {
    contract { returns() implies (this@throwOnFailure is KrosstalkResult.Success<T>) }

    if (this.isFailure()) {
        throwFailureException()
    } else {
        return this.valueOrThrow
    }
}

/**
 * If this is a [HttpError], throw [KrosstalkHttpError].
 */
public fun <T> KrosstalkResult<T>.throwOnHttpError(): KrosstalkResult.SuccessOrServerException<T> {
    contract { returns() implies (this@throwOnHttpError !is KrosstalkResult.HttpError) }

    if (this is KrosstalkResult.HttpError) {
        throwFailureException()
    }
    return this as KrosstalkResult.SuccessOrServerException<T>
}

/**
 * If this is a [ServerException], throw [KrosstalkServerException].
 */
public fun <T> KrosstalkResult<T>.throwOnServerException(): KrosstalkResult.SuccessOrHttpError<T> {
    contract { returns() implies (this@throwOnServerException !is KrosstalkResult.ServerException) }
    if (this is KrosstalkResult.ServerException) {
        throwFailureException()
    }
    return this as KrosstalkResult.SuccessOrHttpError<T>
}

/**
 * Gets the value on success, or the result of [onFailure] otherwise.
 *
 * Alias for `fold({ it }, onFailure)`.
 */
public inline fun <R, T : R> KrosstalkResult<T>.getOrElse(onFailure: (KrosstalkResult.Failure) -> R): R = fold({ it.value }, onFailure)

/**
 * Get the value on success, [onServerException] on a server exception, or [onHttpError] on a http error.
 *
 * Alias for `fold({ it }, onServerException, onHttpError)`.
 */
public inline fun <R, T : R> KrosstalkResult<T>.getOrElse(
    onServerException: (KrosstalkResult.ServerException) -> R,
    onHttpError: (KrosstalkResult.HttpError) -> R,
): R = fold(
    { it.value },
    onServerException = onServerException,
    onHttpError = onHttpError
)

/**
 * Get the value on success, or [onFailure] otherwise.
 *
 * Alias for `fold({ it }, { onFailure })`.
 */
public inline fun <R, T : R> KrosstalkResult<T>.getOrDefault(onFailure: R): R = fold({ it.value }, { onFailure })

/**
 * Get the value on success, [onServerException] on a server exception, or [onHttpError] on a http error.
 *
 * Alias for `fold({ it }, { onServerException }, { onHttpError })`.
 */
public inline fun <R, T : R> KrosstalkResult<T>.getOrDefault(onServerException: R, onHttpError: R): R = fold(
    { it.value },
    onServerException = { onServerException },
    onHttpError = { onHttpError }
)

/**
 * Recover from all server exceptions.  Note that exceptions will not be caught, so [ServerException.throwFailureException] can be used
 * to throw on unhandled server exceptions.
 */
public inline fun <R, T : R> KrosstalkResult<T>.recoverServerExceptions(onServerException: (KrosstalkResult.ServerException) -> R): KrosstalkResult.SuccessOrHttpError<R> =
    when (this) {
        is KrosstalkResult.SuccessOrHttpError -> this
        is KrosstalkResult.ServerException -> KrosstalkResult.Success(onServerException(this))
    }

/**
 * Handle server exceptions matching [filter].
 */
public inline fun <R, T : R> KrosstalkResult<T>.handleServerException(
    filter: (KrosstalkResult.ServerException) -> Boolean,
    onServerException: (KrosstalkResult.ServerException) -> R
): KrosstalkResult<R> =
    when (this) {
        is KrosstalkResult.SuccessOrHttpError -> this
        is KrosstalkResult.ServerException -> if (filter(this)) KrosstalkResult.Success(onServerException(this)) else this
    }

/**
 * Handle server exceptions with a [ServerException.className] of [className].
 */
public inline fun <R, T : R> KrosstalkResult<T>.handleServerException(
    className: String,
    onServerException: (KrosstalkResult.ServerException) -> R
): KrosstalkResult<R> = handleServerException({ it.className == className }, onServerException)

/**
 * Recover from all http errors.  Note that exceptions will not be caught, so [HttpError.throwFailureException] can be used
 * to throw on unhandled http errors.
 */
public inline fun <R, T : R> KrosstalkResult<T>.recoverHttpErrors(onHttpError: (KrosstalkResult.HttpError) -> R): KrosstalkResult.SuccessOrServerException<R> =
    when (this) {
        is KrosstalkResult.SuccessOrServerException -> this
        is KrosstalkResult.HttpError -> KrosstalkResult.Success(onHttpError(this))
    }

/**
 * Handle http errors matching [filter].
 */
public inline fun <R, T : R> KrosstalkResult<T>.handleHttpError(
    filter: (KrosstalkResult.HttpError) -> Boolean,
    onHttpError: (KrosstalkResult.HttpError) -> R
): KrosstalkResult<R> =
    when (this) {
        is KrosstalkResult.SuccessOrServerException -> this
        is KrosstalkResult.HttpError -> if (filter(this)) KrosstalkResult.Success(onHttpError(this)) else this
    }

/**
 * Handle http errors with status codes of [statusCode].
 */
public inline fun <R, T : R> KrosstalkResult<T>.handleHttpError(statusCode: Int, onHttpError: (KrosstalkResult.HttpError) -> R): KrosstalkResult<R> =
    handleHttpError({ it.statusCode == statusCode }, onHttpError)

/**
 * Handle http errors with status codes of [statusCode].
 */
public inline fun <R, T : R> KrosstalkResult<T>.handleHttpError(statusCode: Int, onHttpError: R): KrosstalkResult<R> =
    handleHttpError(statusCode) { onHttpError }

/**
 * Recover from all server exceptions, converting them to a successful value or a http error.
 */
public inline fun <R, T : R> KrosstalkResult<T>.recoverServerExceptionsToResult(onServerException: (KrosstalkResult.ServerException) -> KrosstalkResult.SuccessOrHttpError<R>): KrosstalkResult.SuccessOrHttpError<R> =
    when (this) {
        is KrosstalkResult.ServerException -> onServerException(this)
        is KrosstalkResult.SuccessOrHttpError -> this
    }

/**
 * Handle server exceptions matching [filter] as http errors.
 */
public inline fun <T> KrosstalkResult<T>.handleServerExceptionAsHttpError(
    filter: (KrosstalkResult.ServerException) -> Boolean,
    onServerException: (KrosstalkResult.ServerException) -> KrosstalkResult.HttpError
): KrosstalkResult<T> =
    when (this) {
        is KrosstalkResult.SuccessOrHttpError -> this
        is KrosstalkResult.ServerException -> if (filter(this)) onServerException(this) else this
    }

/**
 * Handle server exceptions with class names of [className] as http errors.
 */
public inline fun <T> KrosstalkResult<T>.handleServerExceptionAsHttpError(
    className: String,
    onServerException: (KrosstalkResult.ServerException) -> KrosstalkResult.HttpError
): KrosstalkResult<T> = handleServerExceptionAsHttpError({ it.className == className }, onServerException)

/**
 * Handle server exceptions matching [filter] as http errors with status code [statusCode] and a message of [ServerException.asString].
 */
public inline fun <T> KrosstalkResult<T>.handleServerExceptionAsHttpError(
    filter: (KrosstalkResult.ServerException) -> Boolean,
    statusCode: Int
): KrosstalkResult<T> =
    handleServerExceptionAsHttpError(filter) { KrosstalkResult.HttpError(statusCode, it.asString) }


/**
 * Handle server exceptions with class names of [className] as http errors with status code [statusCode] and a message of [ServerException.asString].
 */
public inline fun <T> KrosstalkResult<T>.handleServerExceptionAsHttpError(className: String, statusCode: Int): KrosstalkResult<T> =
    handleServerExceptionAsHttpError({ it.className == className }, statusCode)