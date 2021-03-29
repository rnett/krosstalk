package com.rnett.krosstalk

import com.rnett.krosstalk.annotations.ExplicitResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.contracts.contract
import kotlin.reflect.KClass


@OptIn(InternalKrosstalkApi::class)
class ResultHttpErrorException(val httpError: KrosstalkResult.HttpError) :
    KrosstalkException(buildString {
        "KrosstalkResult is http error code ${httpError.statusCode}"
        if (httpError.statusCodeName != null)
            append(": ${httpError.statusCodeName}")

        if (httpError.message != null && !httpError.message.isBlank())
            append(", with message: ${httpError.message}")
    })

@OptIn(InternalKrosstalkApi::class)
class ResultServerExceptionException(val exception: KrosstalkResult.ServerException) : KrosstalkException("KrosstalkResult is exception $exception")

internal expect fun getClassName(klass: KClass<*>): String?

//TODO does not work, https://github.com/Kotlin/kotlinx.serialization/issues/1391
/**
 * The result of a krosstalk call.  Can be either an exception in the server method, a Http error, or success.
 *
 * Used with [ExplicitResult].
 */
@Serializable
sealed class KrosstalkResult<out T> {

    /**
     * The result of a successful krosstalk call.
     *
     * @property value The result.
     */
    @Serializable
    data class Success<out T>(val value: T) : KrosstalkResult<T>()

    //TODO seal, inherit from KrosstalkResult
    /**
     * A failure result.
     */
    interface Failure {
        fun getException(): KrosstalkException
        fun throwException(): Nothing = throw getException()
    }

    //TODO make the failure states inherit Throwable? KrosstalkResult would need to be a sealed interface

    /**
     * The result when an exception occurred during the execution of the server side function
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
    data class ServerException @InternalKrosstalkApi constructor(
        val className: String?,
        val message: String?,
        val cause: ServerException?,
        val suppressed: List<ServerException>,
        val asString: String,
        val asStringWithStacktrace: String?,
        @Transient @InternalKrosstalkApi val throwable: Throwable? = null,
    ) : KrosstalkResult<Nothing>(), Failure {

        @InternalKrosstalkApi
        constructor(throwable: Throwable, includeStacktrace: Boolean) : this(
            getClassName(throwable::class),
            throwable.message,
            throwable.cause?.let { ServerException(it, includeStacktrace) },
            throwable.suppressedExceptions.map { ServerException(it, includeStacktrace) },
            throwable.toString(),
            if (includeStacktrace) throwable.stackTraceToString() else null,
            throwable
        )

        override fun getException() = ResultServerExceptionException(this)
    }

    //TODO look at message, do I want it to be the returned body?
    /**
     * A non-success HTTP response was gotten from the krosstalk call.
     *
     * @property statusCode The HTTP response code of the erroneous response.
     * @property clientMessage Any message that the KrosstalkClient associated with the response
     */
    @Serializable
    data class HttpError @InternalKrosstalkApi constructor(val statusCode: Int, val statusCodeName: String?, val message: String?) :
        KrosstalkResult<Nothing>(),
        Failure {
        override fun getException() = ResultHttpErrorException(this)
    }

    /**
     * Whether the result is success.
     */
    fun isSuccess(): Boolean {
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
    val valueOrNull get() = getOrDefault(null)

    /**
     * Get the value if successful, otherwise throw with [throwOnFailure]
     */
    val valueOrThrow get() = getOrElse { it.throwException() }

    /**
     * Whether the result is a server exception.
     */
    fun isServerException(): Boolean {
        contract {
            returns(true) implies (this@KrosstalkResult is ServerException)
            returns(false) implies (this@KrosstalkResult !is ServerException)
        }
        return this is ServerException
    }

    /**
     * Get the server exception if there is one, else null
     */
    val serverExceptionOrNull: ServerException? get() = if (this is ServerException) this else null

    /**
     * Whether the result is a http error.
     */
    fun isHttpError(): Boolean {
        contract {
            returns(true) implies (this@KrosstalkResult is HttpError)
            returns(false) implies (this@KrosstalkResult !is HttpError)
        }
        return this is HttpError
    }

    /**
     * Get the http error if there is one, else null
     */
    val httpErrorOrNull: HttpError? get() = if (this is HttpError) this else null

    /**
     * Whether the result is not a success.
     */
    fun isFailure(): Boolean {
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
    val failureOrNull get() = if (this is Failure) this else null

    /**
     * If this is a [HttpError], throw [ResultHttpErrorException].  If this is a [ServerException], throw [ResultServerExceptionException].
     */
    fun throwOnFailure(): KrosstalkResult<T> {
        // TODO use contracts, blocked by https://youtrack.jetbrains.com/issue/KT-41078
        // contract { returns() implies (this@KrosstalkCallResult is Success) }

        if (this is Failure) {
            throwException()
        }
        return this
    }

    /**
     * If this is a [HttpError], throw [ResultHttpErrorException].
     */
    fun throwOnHttpError(): KrosstalkResult<T> {
        contract { returns() implies (this@KrosstalkResult !is HttpError) }

        if (this is HttpError) {
            throwException()
        }
        return this
    }

    /**
     * If this is a [ServerException], throw [ResultServerExceptionException].
     */
    fun throwOnServerException(): KrosstalkResult<T> {
        contract { returns() implies (this@KrosstalkResult !is ServerException) }

        if (this is ServerException) {
            throwException()
        }
        return this
    }

    inline fun <R> map(transform: (T) -> R): KrosstalkResult<R> = when (this) {
        is Success -> success(transform(value))
        is ServerException -> this
        is HttpError -> this
    }

    inline fun <R> fold(onSuccess: (T) -> R, onFailure: (Failure) -> R): R = when (this) {
        is Success -> onSuccess(value)
        is ServerException -> onFailure(this)
        is HttpError -> onFailure(this)
    }

    inline fun <R> fold(
        onSuccess: (T) -> R,
        onServerException: (ServerException) -> R,
        onHttpError: (HttpError) -> R,
    ): R = when (this) {
        is Success -> onSuccess(value)
        is HttpError -> onHttpError(this)
        is ServerException -> onServerException(this)
    }

    //TODO once we can use result
    //fun toResult(): Result<T> = fold({ Result.success(it) }, { Result.failure(it.getException()) })

    companion object {
        fun <T> success(value: T) = Success(value)
        operator fun <T> invoke(value: T) = success(value)
    }

    //TODO new API using sealed interfaces and contracts once 1.5 is out
    //TODO toResult() or similar
}

/**
 * Gets the value on success, or the result of [onFailure] otherwise.
 *
 * Alias for `fold({ it }, onFailure)`.
 */
inline fun <R, T : R> KrosstalkResult<T>.getOrElse(onFailure: (KrosstalkResult.Failure) -> R): R = fold({ it }, onFailure)

/**
 * Get the value on success, [onServerException] on a server exception, or [onHttpError] on a http error.
 *
 * Alias for `fold({ it }, onServerException, onHttpError)`.
 */
inline fun <R, T : R> KrosstalkResult<T>.getOrElse(
    onServerException: (KrosstalkResult.ServerException) -> R,
    onHttpError: (KrosstalkResult.HttpError) -> R,
): R = fold(
    { it },
    onServerException = onServerException,
    onHttpError = onHttpError)

/**
 * Get the value on success, or [onFailure] otherwise.
 *
 * Alias for `fold({ it }, { onFailure })`.
 */
inline fun <R, T : R> KrosstalkResult<T>.getOrDefault(onFailure: R): R = fold({ it }, { onFailure })

/**
 * Get the value on success, [onServerException] on a server exception, or [onHttpError] on a http error.
 *
 * Alias for `fold({ it }, { onServerException }, { onHttpError })`.
 */
inline fun <R, T : R> KrosstalkResult<T>.getOrDefault(onServerException: R, onHttpError: R): R = fold(
    { it },
    onServerException = { onServerException },
    onHttpError = { onHttpError }
)

/**
 * Recover from some failures.  Successes will be preserved, otherwise the result will be [onFailure] called on [this].
 */
inline fun <R, T : R> KrosstalkResult<T>.recover(onFailure: (KrosstalkResult.Failure) -> KrosstalkResult<R>): KrosstalkResult<R> = when (this) {
    is KrosstalkResult.Success -> this
    is KrosstalkResult.ServerException -> onFailure(this)
    is KrosstalkResult.HttpError -> onFailure(this)
}

/**
 * Recover from some failures.  Successes will be presented, and if [onFailure] successfully completes, a success will be returned.
 * If [onFailure] throws a [ResultServerExceptionException] or [ResultHttpErrorException], like from [KrosstalkResult.Failure.throwException],
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
inline fun <R, T : R> KrosstalkResult<T>.recoverCatching(onFailure: (KrosstalkResult.Failure) -> R): KrosstalkResult<R> = recover {
    try {
        KrosstalkResult.success(onFailure(it))
    } catch (e: ResultServerExceptionException) {
        e.exception
    } catch (e: ResultHttpErrorException) {
        e.httpError
    }
}

/**
 * Recover from some failures.  Successes will be preserved, otherwise the result will be [onFailure] called on [this].
 */
inline fun <R, T : R> KrosstalkResult<T>.recoverServerException(onServerException: (KrosstalkResult.ServerException) -> KrosstalkResult<R>) =
    recover {
        if (it is KrosstalkResult.ServerException)
            onServerException(it)
        else
            it as KrosstalkResult<R>
    }

/**
 * Recover from some failures.  Successes will be presented, and if [onServerException] successfully completes, a success will be returned.
 * If [onServerException] throws a [ResultServerExceptionException], like from [KrosstalkResult.ServerException.throwException],
 * the originating result will be used.
 *
 * @see recoverCatching
 */
inline fun <R, T : R> KrosstalkResult<T>.recoverServerExceptionCatching(onServerException: (KrosstalkResult.ServerException) -> R) = recover {
    if (it is KrosstalkResult.ServerException) {
        try {
            KrosstalkResult.success(onServerException(it))
        } catch (e: ResultServerExceptionException) {
            e.exception
        }
    } else
        it as KrosstalkResult<R>
}

/**
 * Recover from some failures.  Successes will be preserved, otherwise the result will be [onFailure] called on [this].
 */
inline fun <R, T : R> KrosstalkResult<T>.recoverHttpError(onHttpError: (KrosstalkResult.HttpError) -> KrosstalkResult<R>) = recover {
    if (it is KrosstalkResult.HttpError)
        onHttpError(it)
    else
        it as KrosstalkResult<R>
}

/**
 * Recover from some failures.  Successes will be presented, and if [onServerException] successfully completes, a success will be returned.
 * If [onServerException] throws a [ResultServerExceptionException], like from [KrosstalkResult.ServerException.throwException],
 * the originating result will be used.
 *
 * @see recoverCatching
 */
inline fun <R, T : R> KrosstalkResult<T>.recoverHttpErrorCatching(onHttpError: (KrosstalkResult.HttpError) -> R) = recover {
    if (it is KrosstalkResult.HttpError) {
        try {
            KrosstalkResult.success(onHttpError(it))
        } catch (e: ResultHttpErrorException) {
            e.httpError
        }
    } else
        it as KrosstalkResult<R>
}

/**
 * Recover a http error code.  Successes will be preserved, and if the result is a [KrosstalkResult.HttpError] with [responseCode], a success with
 * the value of [onHttpError] will be used to get the result.
 */
inline fun <R, T : R> KrosstalkResult<T>.recoverHttpError(responseCode: Int, onError: (KrosstalkResult.HttpError) -> R) = recover {
    if (it is KrosstalkResult.HttpError && it.statusCode == responseCode)
        KrosstalkResult.success(onError(it))
    else
        it as KrosstalkResult<R>
}

/**
 * Recover a http error code.  Successes will be preserved, and if the result is a [KrosstalkResult.HttpError] with [responseCode], a success with
 * the value of [onHttpError] will be used.
 */
inline fun <R, T : R> KrosstalkResult<T>.recoverHttpError(responseCode: Int, onError: R) = recover {
    if (it is KrosstalkResult.HttpError && it.statusCode == responseCode)
        KrosstalkResult.success(onError)
    else
        it as KrosstalkResult<R>
}

/**
 * Execute [onSuccess] if the result is a success.
 */
inline fun <T> KrosstalkResult<T>.onSuccess(onSuccess: (T) -> Unit) = also {
    if (it is KrosstalkResult.Success)
        onSuccess(it.value)
}

/**
 * Execute [onFailure] if the result is a failure.
 */
inline fun <T> KrosstalkResult<T>.onFailure(onFailure: (KrosstalkResult.Failure) -> Unit) = also {
    if (it.isFailure())
        onFailure(it)
}

/**
 * Execute [onServerException] if the result is a server exception.
 */
inline fun <T> KrosstalkResult<T>.onServerException(onServerException: (KrosstalkResult.ServerException) -> Unit) = also {
    if (it.isServerException())
        onServerException(it)
}

/**
 * Execute [onHttpError] if the result is a http error.
 */
inline fun <T> KrosstalkResult<T>.onHttpError(onHttpError: (KrosstalkResult.HttpError) -> Unit) = also {
    if (it.isHttpError())
        onHttpError(it)
}