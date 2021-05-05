package com.rnett.krosstalk

import com.rnett.krosstalk.annotations.ExplicitResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass


@OptIn(InternalKrosstalkApi::class)
public class KrosstalkResultHttpError(public val httpError: KrosstalkResult.HttpError) :
    KrosstalkException(buildString {
        "KrosstalkResult is http error code ${httpError.statusCode}"
        if (httpError.statusCodeName != null)
            append(": ${httpError.statusCodeName}")

        if (httpError.message != null && !httpError.message.isBlank())
            append(", with message: ${httpError.message}")
    })

public inline fun throwKrosstalkHttpError(statusCode: Int, message: String? = null): Nothing {
    throw KrosstalkResultHttpError(KrosstalkResult.HttpError(statusCode, message))
}

@OptIn(InternalKrosstalkApi::class)
public class KrosstalkServerException(public val exception: KrosstalkResult.ServerException) :
    KrosstalkException("KrosstalkResult is exception $exception")

@OptIn(InternalKrosstalkApi::class)
public inline fun throwKrosstalkServerException(throwable: Throwable, includeStackTrace: Boolean = true): Nothing {
    throw KrosstalkServerException(KrosstalkResult.ServerException(throwable, includeStackTrace))
}


internal expect fun getClassName(klass: KClass<*>): String?

/**
 * Runs [block] and wraps the result if it is a success.  If [block] throws,
 * it wraps the resulting exception in [KrosstalkResult.ServerException].
 *
 * If the caught exception is [KrosstalkServerException] or [KrosstalkResultHttpError],
 * they are unwrapped instead of wrapped (i.e. their contained [KrosstalkResult] is returned).
 *
 * Note that [includeStackTrace] will be overridden by the vale in any [ExplicitResult] annotations,
 * and thus defaults to `true`.
 */
@OptIn(InternalKrosstalkApi::class)
public inline fun <T> runKrosstalkCatching(includeStackTrace: Boolean = true, block: () -> T): KrosstalkResult<T> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return try {
        KrosstalkResult.Success(block())
    } catch (e: KrosstalkServerException) {
        return e.exception
    } catch (e: KrosstalkResultHttpError) {
        return e.httpError
    } catch (t: Throwable) {
        KrosstalkResult.ServerException(t, includeStackTrace)
    }
}

/**
 * Convert a [Result] to a [KrosstalkResult], useing [KrosstalkResult.ServerException] to represent a failure.
 *
 * Note that [includeStackTrace] will be overridden by the vale in any [ExplicitResult] annotations,
 * and thus defaults to `true`.
 */
@OptIn(InternalKrosstalkApi::class)
public inline fun <T> Result<T>.toKrosstalkResult(includeStackTrace: Boolean = true): KrosstalkResult<T> {
    return if (this.isSuccess)
        KrosstalkResult.Success(this.getOrThrow())
    else
        KrosstalkResult.ServerException(this.exceptionOrNull()!!, includeStackTrace)
}

/**
 * The result of a krosstalk call.  Can be either an exception in the server method, a Http error, or success.
 *
 * Used with [ExplicitResult].
 */
@Serializable
public sealed class KrosstalkResult<out T> {

    /**
     * The result of a successful krosstalk call.
     *
     * Should not be created directly, use [runKrosstalkCatching].
     *
     * @property value The result.
     */
    @Serializable
    public data class Success<out T> @InternalKrosstalkApi constructor(val value: T) : KrosstalkResult<T>()

    //TODO seal, inherit from KrosstalkResult
    /**
     * A failure result.
     */
    public interface Failure {
        public fun getException(): KrosstalkException
        public fun throwException(): Nothing = throw getException()
    }

    //TODO make the failure states inherit Throwable? KrosstalkResult would need to be a sealed interface

    /**
     * The result when an exception occurred during the execution of the server side function.
     *
     * Should not be created directly, use [runKrosstalkCatching].
     *
     * @property className the name of the exception class, or null if it can't be obtained.
     * Will be [KClass.qualifiedName] when it is available on the server (i.e. on a JVM server), and [KClass.simpleName] when it is not.
     * @property message the [Throwable.message] of the exception, if it has one
     * @property cause the exception's [Throwable.cause], if it has one
     * @property suppressed the exception's [Throwable.suppressedExceptions].
     * @property asString the exception's [Throwable.toString]
     * @property asStringWithStacktrace the exception's [Throwable.stackTraceToString], if it is included (See [ExplicitResult]).
     */
    @Serializable
    public data class ServerException internal constructor(
        val className: String?,
        val message: String?,
        val cause: ServerException?,
        val suppressed: List<ServerException>,
        val asString: String,
        val asStringWithStacktrace: String?,
        @Transient @InternalKrosstalkApi val throwable: Throwable? = null,
    ) : KrosstalkResult<Nothing>(), Failure {

        @InternalKrosstalkApi
        public constructor(throwable: Throwable, includeStacktrace: Boolean) : this(
            getClassName(throwable::class),
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

        override fun getException(): KrosstalkServerException = KrosstalkServerException(this)
    }

    /**
     * A non-success HTTP response was gotten from the krosstalk call.
     *
     * @property statusCode The HTTP response code of the erroneous response.
     * @property message Any message that the KrosstalkClient associated with the response
     */
    @Serializable
    public data class HttpError(val statusCode: Int, val message: String? = null) :
        KrosstalkResult<Nothing>(),
        Failure {
        override fun getException(): KrosstalkResultHttpError = KrosstalkResultHttpError(this)

        /**
         * The name of [statusCode].  Generated from [https://developer.mozilla.org/en-US/docs/Web/HTTP/Status](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status).
         */
        @OptIn(InternalKrosstalkApi::class)
        val statusCodeName: String? by lazy { httpStatusCodes[statusCode] }
    }

    /**
     * Whether the result is success.
     */
    public fun isSuccess(): Boolean {
        contract {
            // TODO use contracts, blocked by https://youtrack.jetbrains.com/issue/KT-41078
//             returns(true) implies (this@KrosstalkResult is Success)
            returns(false) implies (this@KrosstalkResult is Failure)
        }
        return this is Success
    }

    /**
     * Get the value if successful, else null
     */
    public val valueOrNull: T? get() = getOrDefault(null)

    /**
     * Get the value if successful, otherwise throw with [throwOnFailure]
     */
    public val valueOrThrow: T get() = getOrElse { it.throwException() }

    /**
     * Whether the result is a server exception.
     */
    public fun isServerException(): Boolean {
        contract {
            returns(true) implies (this@KrosstalkResult is ServerException)
            returns(false) implies (this@KrosstalkResult !is ServerException)
        }
        return this is ServerException
    }

    /**
     * Get the server exception if there is one, else null
     */
    public val serverExceptionOrNull: ServerException? get() = if (this is ServerException) this else null

    /**
     * Whether the result is a http error.
     */
    public fun isHttpError(): Boolean {
        contract {
            returns(true) implies (this@KrosstalkResult is HttpError)
            returns(false) implies (this@KrosstalkResult !is HttpError)
        }
        return this is HttpError
    }

    /**
     * Get the http error if there is one, else null
     */
    public val httpErrorOrNull: HttpError? get() = if (this is HttpError) this else null

    /**
     * Whether the result is not a success.
     */
    public fun isFailure(): Boolean {
        contract {
            returns(true) implies (this@KrosstalkResult is Failure)
            // TODO use contracts, blocked by https://youtrack.jetbrains.com/issue/KT-41078
//            returns(false) implies (this@KrosstalkResult is Success)
        }
        return this is Failure
    }

    /**
     * Get the failure if there is one, else null
     */
    public val failureOrNull: Any? get() = if (this is Failure) this else null

    /**
     * If this is a [HttpError], throw [KrosstalkResultHttpError].  If this is a [ServerException], throw [KrosstalkServerException].
     */
    public fun throwOnFailure(): KrosstalkResult<T> {
        // TODO use contracts, blocked by https://youtrack.jetbrains.com/issue/KT-41078
        // contract { returns() implies (this@KrosstalkCallResult is Success) }

        if (this is Failure) {
            throwException()
        }
        return this
    }

    /**
     * If this is a [HttpError], throw [KrosstalkResultHttpError].
     */
    public fun throwOnHttpError(): KrosstalkResult<T> {
        contract { returns() implies (this@KrosstalkResult !is HttpError) }

        if (this is HttpError) {
            throwException()
        }
        return this
    }

    /**
     * If this is a [ServerException], throw [KrosstalkServerException].
     */
    public fun throwOnServerException(): KrosstalkResult<T> {
        contract { returns() implies (this@KrosstalkResult !is ServerException) }

        if (this is ServerException) {
            throwException()
        }
        return this
    }

    @OptIn(InternalKrosstalkApi::class)
    public inline fun <R> map(transform: (T) -> R): KrosstalkResult<R> = when (this) {
        is Success -> Success(transform(value))
        is ServerException -> this
        is HttpError -> this
    }

    public inline fun <R> fold(onSuccess: (T) -> R, onFailure: (Failure) -> R): R = when (this) {
        is Success -> onSuccess(value)
        is ServerException -> onFailure(this)
        is HttpError -> onFailure(this)
    }

    public inline fun <R> fold(
        onSuccess: (T) -> R,
        onServerException: (ServerException) -> R,
        onHttpError: (HttpError) -> R,
    ): R = when (this) {
        is Success -> onSuccess(value)
        is HttpError -> onHttpError(this)
        is ServerException -> onServerException(this)
    }

    //TODO move both of these, and `catch` to krosstalk-server once we don't have to specify all type parameters

    /**
     * Re-throw the exception of a [KrosstalkResult.ServerException] if the exception is known, of type [E], and passes [predicate] (which is true by default).
     *
     * **Note:** this function will most likely only work on server side, where the exception is known.
     */
    public inline fun <reified E : Throwable> throwServerException(predicate: (E) -> Boolean = { true }): KrosstalkResult<T> {
        if (this is ServerException && this.throwable is E && predicate(this.throwable))
            throw this.throwable
        else
            return this
    }

    /**
     * Catch a [KrosstalkResult.ServerException] where the exception is known and is of type [E], converting it to a [KrosstalkResult.HttpError].
     *
     * If [httpError] returns null, the server exception is not converted.
     *
     * **Note:** this function will most likely only work on server side, where the exception is known.
     */
    @OptIn(InternalKrosstalkApi::class)
    public inline fun <reified E : Throwable> catchAsHttpError(httpError: (E) -> HttpError?): KrosstalkResult<T> = when (this) {
        is Success -> this
        is ServerException -> {
            if (throwable is E)
                httpError(throwable) ?: this
            else
                this
        }
        is HttpError -> this
    }

    /**
     * Catch a [KrosstalkResult.ServerException] where the exception is known and is of type [E], converting it to a [KrosstalkResult.HttpError].
     * This uses the status code returned from [statusCode] with the message from the exception.
     *
     * If [statusCode] returns null, the server exception is not converted.
     *
     * **Note:** this function will most likely only work on server side, where the exception is known.
     */
    @OptIn(InternalKrosstalkApi::class)
    public inline fun <reified E : Throwable> catchAsHttpStatusCode(statusCode: (E) -> Int?): KrosstalkResult<T> = when (this) {
        is Success -> this
        is ServerException -> {
            if (throwable is E)
                statusCode(throwable)?.let {
                    HttpError(it, throwable.message)
                } ?: this
            else
                this
        }
        is HttpError -> this
    }

    //TODO once we can use result
    //fun toResult(): Result<T> = fold({ Result.success(it) }, { Result.failure(it.getException()) })

    //TODO new API using sealed interfaces and contracts once 1.5 is out
    //TODO toResult() or similar
}

/**
 * Catch a [KrosstalkResult.ServerException] where the exception is known and is of type [E], converting it to a success.
 *
 * If [predicate] returns false, the server exception is not converted (it is true by default).
 *
 * **Note:** this function will most likely only work on server side, where the exception is known.
 */
@OptIn(InternalKrosstalkApi::class)
public inline fun <reified E : Throwable, R, T : R> KrosstalkResult<T>.catch(
    predicate: (E) -> Boolean = { true },
    block: (E) -> R,
): KrosstalkResult<R> =
    when (this) {
        is KrosstalkResult.Success -> this
        is KrosstalkResult.ServerException -> if (throwable is E && predicate(throwable as E)) KrosstalkResult.Success(block(throwable as E)) else this
        is KrosstalkResult.HttpError -> this
    }

/**
 * Gets the value on success, or the result of [onFailure] otherwise.
 *
 * Alias for `fold({ it }, onFailure)`.
 */
public inline fun <R, T : R> KrosstalkResult<T>.getOrElse(onFailure: (KrosstalkResult.Failure) -> R): R = fold({ it }, onFailure)

/**
 * Get the value on success, [onServerException] on a server exception, or [onHttpError] on a http error.
 *
 * Alias for `fold({ it }, onServerException, onHttpError)`.
 */
public inline fun <R, T : R> KrosstalkResult<T>.getOrElse(
    onServerException: (KrosstalkResult.ServerException) -> R,
    onHttpError: (KrosstalkResult.HttpError) -> R,
): R = fold(
    { it },
    onServerException = onServerException,
    onHttpError = onHttpError
)

/**
 * Get the value on success, or [onFailure] otherwise.
 *
 * Alias for `fold({ it }, { onFailure })`.
 */
public inline fun <R, T : R> KrosstalkResult<T>.getOrDefault(onFailure: R): R = fold({ it }, { onFailure })

/**
 * Get the value on success, [onServerException] on a server exception, or [onHttpError] on a http error.
 *
 * Alias for `fold({ it }, { onServerException }, { onHttpError })`.
 */
public inline fun <R, T : R> KrosstalkResult<T>.getOrDefault(onServerException: R, onHttpError: R): R = fold(
    { it },
    onServerException = { onServerException },
    onHttpError = { onHttpError }
)

/**
 * Recover from some failures.  Successes will be preserved, otherwise the result will be [onFailure] called on [this].
 */
public inline fun <R, T : R> KrosstalkResult<T>.recover(onFailure: (KrosstalkResult.Failure) -> KrosstalkResult<R>): KrosstalkResult<R> =
    when (this) {
        is KrosstalkResult.Success -> this
        is KrosstalkResult.ServerException -> onFailure(this)
        is KrosstalkResult.HttpError -> onFailure(this)
    }

/**
 * Recover from some failures.  Successes will be presented, and if [onFailure] successfully completes, a success will be returned.
 * If [onFailure] throws a [KrosstalkServerException] or [KrosstalkResultHttpError], like from [KrosstalkResult.Failure.throwException],
 * the originating result will be used.
 *
 * For example:
 * ```kotlin
 * result.recoverCatching{
 *     if(it is KrosstalkResult.HttpError && it.responseCode == 404)
 *         null
 *     else
 *         it.throwException()
 * }
 * ```
 * will result in a `KrosstalkResult<T?>` that is the success value if success, null if it was a HttpError with 404, or the failure otherwise.
 */
@OptIn(InternalKrosstalkApi::class)
public inline fun <R, T : R> KrosstalkResult<T>.recoverCatching(onFailure: (KrosstalkResult.Failure) -> R): KrosstalkResult<R> = recover {
    try {
        KrosstalkResult.Success(onFailure(it))
    } catch (e: KrosstalkServerException) {
        e.exception
    } catch (e: KrosstalkResultHttpError) {
        e.httpError
    }
}

/**
 * Recover from some failures.  Successes will be preserved, otherwise the result will be [onFailure] called on [this].
 */
public inline fun <R, T : R> KrosstalkResult<T>.recoverServerException(onServerException: (KrosstalkResult.ServerException) -> KrosstalkResult<R>): KrosstalkResult<R> =
    recover {
        if (it is KrosstalkResult.ServerException)
            onServerException(it)
        else
            it as KrosstalkResult<R>
    }

/**
 * Recover from some failures.  Successes will be presented, and if [onServerException] successfully completes, a success will be returned.
 * If [onServerException] throws a [KrosstalkServerException], like from [KrosstalkResult.ServerException.throwException],
 * the originating result will be used.
 *
 * @see recoverCatching
 */
@OptIn(InternalKrosstalkApi::class)
public inline fun <R, T : R> KrosstalkResult<T>.recoverServerExceptionCatching(onServerException: (KrosstalkResult.ServerException) -> R): KrosstalkResult<R> =
    recover {
        if (it is KrosstalkResult.ServerException) {
            try {
                KrosstalkResult.Success(onServerException(it))
            } catch (e: KrosstalkServerException) {
                e.exception
            }
        } else
            it as KrosstalkResult<R>
    }

/**
 * Recover from some failures.  Successes will be preserved, otherwise the result will be [onFailure] called on [this].
 */
public inline fun <R, T : R> KrosstalkResult<T>.recoverHttpError(onHttpError: (KrosstalkResult.HttpError) -> KrosstalkResult<R>): KrosstalkResult<R> =
    recover {
        if (it is KrosstalkResult.HttpError)
            onHttpError(it)
        else
            it as KrosstalkResult<R>
    }

/**
 * Recover from some failures.  Successes will be presented, and if [onServerException] successfully completes, a success will be returned.
 * If [onServerException] throws a [KrosstalkServerException], like from [KrosstalkResult.ServerException.throwException],
 * the originating result will be used.
 *
 * @see recoverCatching
 */
@OptIn(InternalKrosstalkApi::class)
public inline fun <R, T : R> KrosstalkResult<T>.recoverHttpErrorCatching(onHttpError: (KrosstalkResult.HttpError) -> R): KrosstalkResult<R> =
    recover {
        if (it is KrosstalkResult.HttpError) {
            try {
                KrosstalkResult.Success(onHttpError(it))
            } catch (e: KrosstalkResultHttpError) {
                e.httpError
            }
        } else
            it as KrosstalkResult<R>
    }

/**
 * Recover a http error code.  Successes will be preserved, and if the result is a [KrosstalkResult.HttpError] with [responseCode], a success with
 * the value of [onHttpError] will be used to get the result.
 */
@OptIn(InternalKrosstalkApi::class)
public inline fun <R, T : R> KrosstalkResult<T>.recoverHttpError(responseCode: Int, onError: (KrosstalkResult.HttpError) -> R): KrosstalkResult<R> =
    recover {
        if (it is KrosstalkResult.HttpError && it.statusCode == responseCode)
            KrosstalkResult.Success(onError(it))
        else
            it as KrosstalkResult<R>
    }

/**
 * Recover a http error code.  Successes will be preserved, and if the result is a [KrosstalkResult.HttpError] with [responseCode], a success with
 * the value of [onHttpError] will be used.
 */
@OptIn(InternalKrosstalkApi::class)
public inline fun <R, T : R> KrosstalkResult<T>.recoverHttpError(responseCode: Int, onError: R): KrosstalkResult<R> = recover {
    if (it is KrosstalkResult.HttpError && it.statusCode == responseCode)
        KrosstalkResult.Success(onError)
    else
        it as KrosstalkResult<R>
}

/**
 * Execute [onSuccess] if the result is a success.
 */
public inline fun <T> KrosstalkResult<T>.onSuccess(onSuccess: (T) -> Unit): KrosstalkResult<T> = also {
    if (it is KrosstalkResult.Success)
        onSuccess(it.value)
}

/**
 * Execute [onFailure] if the result is a failure.
 */
public inline fun <T> KrosstalkResult<T>.onFailure(onFailure: (KrosstalkResult.Failure) -> Unit): KrosstalkResult<T> = also {
    if (it.isFailure())
        onFailure(it)
}

/**
 * Execute [onServerException] if the result is a server exception.
 */
public inline fun <T> KrosstalkResult<T>.onServerException(onServerException: (KrosstalkResult.ServerException) -> Unit): KrosstalkResult<T> = also {
    if (it.isServerException())
        onServerException(it)
}

/**
 * Execute [onHttpError] if the result is a http error.
 */
public inline fun <T> KrosstalkResult<T>.onHttpError(onHttpError: (KrosstalkResult.HttpError) -> Unit): KrosstalkResult<T> = also {
    if (it.isHttpError())
        onHttpError(it)
}