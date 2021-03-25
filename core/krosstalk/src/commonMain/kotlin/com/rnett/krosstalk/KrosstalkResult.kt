package com.rnett.krosstalk

import com.rnett.krosstalk.annotations.ExplicitResult
import kotlinx.serialization.Serializable
import kotlin.contracts.contract


class ResultHttpErrorException(val responseCode: Int, val clientMessage: String?) :
    KrosstalkException("KrosstalkResult is http error code $responseCode, with message $clientMessage")

class ResultServerExceptionException(val exception: KrosstalkFailure.ServerException) : KrosstalkException("KrosstalkResult is exception $exception")


@Serializable
sealed class KrosstalkFailure {

    abstract fun throwException(): Nothing

    /**
     * A non-success HTTP response was gotten from the krosstalk call.
     *
     * @property responseCode The HTTP response code of the erroneous response.
     * @property clientMessage Any message that the KrosstalkClient associated with the response
     */
    @Serializable
    data class HttpError @PublishedApi internal constructor(val responseCode: Int, val clientMessage: String? = null) : KrosstalkFailure() {
        override fun throwException(): Nothing {
            throw ResultHttpErrorException(responseCode, clientMessage)
        }
    }

    /**
     * @property className the name of the exception class, or null if it can't be obtained
     * @property message the [Throwable.message] of the exception, if it has one
     * @property cause the exception's [Throwable.cause], if it has one
     * @property suppressed the exception's [Throwable.suppressedExceptions].
     * @property asString the exception's [Throwable.toString]
     * @property asStringWithStacktrace the exception's [Throwable.stackTraceToString], if it is included (See [ExplicitResult]).
     */
    @Serializable
    @Suppress("DataClassPrivateConstructor")
    data class ServerException private constructor(
        val className: String?,
        val message: String?,
        val cause: ServerException?,
        val suppressed: List<ServerException>,
        val asString: String,
        val asStringWithStacktrace: String?,
    ) : KrosstalkFailure() {
        constructor(throwable: Throwable, includeStacktrace: Boolean) : this(
            throwable::class.qualifiedName,
            throwable.message,
            throwable.cause?.let { ServerException(it, includeStacktrace) },
            throwable.suppressedExceptions.map { ServerException(it, includeStacktrace) },
            throwable.toString(),
            if (includeStacktrace) throwable.stackTraceToString() else null
        )

        override fun throwException(): Nothing {
            throw ResultServerExceptionException(this)
        }
    }
}

typealias KrosstalkResult<T> = KrosstalkCallResult<T, KrosstalkFailure>

inline fun <reified T> Any?.isType(): Boolean {
    contract { returns(true) implies (this@isType is T) }
    return this is T
}

/**
 * The result of a krosstalk call.  Can be either an exception in the server method, a Http error, or success.
 *
 * Used with [ExplicitResult].
 */
@Serializable
sealed class KrosstalkCallResult<out T, out F : KrosstalkFailure> {

    /**
     * The result of a successful krosstalk call.
     *
     * @property value The result.
     */
    @Serializable
    data class Success<T>(val value: T) : KrosstalkCallResult<T, Nothing>()

    @Serializable
    data class Failure<F : KrosstalkFailure>(val failure: F) : KrosstalkCallResult<Nothing, F>()

    /**
     * Whether the result is success.
     */
    fun isSuccess(): Boolean {
        // TODO use contracts, blocked by https://youtrack.jetbrains.com/issue/KT-41078
        // contract { returns(true) implies (this@KrosstalkCallResult is Success) }
        return this is Success
    }

    /**
     * Get the value if successful, else null
     */
    val valueOrNull get() = if (this is Success) this.value else null

    /**
     * Whether the result is a http error.
     */
    fun isHttpError(): Boolean {
        // TODO use contracts, blocked by https://youtrack.jetbrains.com/issue/KT-41078
        // contract { returns(true) implies (this@KrosstalkCallResult is HttpError) }
        return this is Failure && this.failure is KrosstalkFailure.HttpError
    }

    /**
     * Get the http error if there is one, else null
     */
    val httpErrorOrNull: KrosstalkFailure.HttpError? get() = if (this is Failure && failure is KrosstalkFailure.HttpError) failure else null

    /**
     * Whether the result is success.
     */
    fun isServerException(): Boolean {
        // TODO use contracts, blocked by https://youtrack.jetbrains.com/issue/KT-41078
        // contract { returns(true) implies (this@KrosstalkCallResult is Exception) }
        return this is Failure && this.failure is KrosstalkFailure.ServerException
    }

    //TODO I don't like getting the property for only this one
    /**
     * Get the exception if there is one, else null
     */
    val serverExceptionOrNull: KrosstalkFailure.ServerException? get() = if (this is Failure && failure is KrosstalkFailure.ServerException) failure else null

    /**
     * Whether the result is not a success.
     */
    fun isFailure(): Boolean = this is Failure

    /**
     * Get the failure if there is one, else null
     */
    val failureOrNull get() = if (this is Failure) failure else null

    /**
     * If this is a [HttpError], throw [ResultHttpErrorException].  If this is a [Exception], throw [ResultServerExceptionException].
     */
    fun throwOnFailure() {
        // TODO use contracts, blocked by https://youtrack.jetbrains.com/issue/KT-41078
        // contract { returns() implies (this@KrosstalkCallResult is Success) }

        if (this is Failure) {
            failure.throwException()
        }
    }

    /**
     * If this is a [HttpError], throw [ResultHttpErrorException].
     */
    fun throwOnHttpError() {
        // TODO use contracts, blocked by https://youtrack.jetbrains.com/issue/KT-41078

        if (this is Failure && failure is KrosstalkFailure.HttpError) {
            failure.throwException()
        }
    }

    /**
     * If this is a [Exception], throw [ResultServerExceptionException].
     */
    fun throwOnServerException() {
        // TODO use contracts, blocked by https://youtrack.jetbrains.com/issue/KT-41078

        if (this is Failure && failure is KrosstalkFailure.ServerException) {
            failure.throwException()
        }
    }

    /**
     * Get the value if successful, otherwise throw with [throwOnFailure]
     */
    val valueOrThrow
        get(): T {
            throwOnFailure()
            return (this as Success).value
        }

    inline fun <R> map(transform: (T) -> R): KrosstalkCallResult<R, F> = when (this) {
        is Success -> success(transform(value))
        is Failure -> this
    }

    inline fun <R> fold(onSuccess: (T) -> R, onFailure: (F) -> R): R = when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(failure)
    }

    inline fun <R> fold(
        onSuccess: (T) -> R,
        onHttpError: (KrosstalkFailure.HttpError) -> R,
        onServerException: (KrosstalkFailure.ServerException) -> R,
    ): R =
        fold(
            onSuccess,
            onFailure = {
                when (val f = it as KrosstalkFailure) {
                    is KrosstalkFailure.HttpError -> onHttpError(f)
                    is KrosstalkFailure.ServerException -> onServerException(f)
                }
            }
        )

//    fun toResult(): Result<T> = when(this){
//
//    }

    companion object {
        fun <T> success(value: T) = Success(value)
        fun <F : KrosstalkFailure> failure(failure: F) = Failure(failure)
        operator fun <T> invoke(value: T) = success(value)
    }

    //TODO new API using sealed interfaces and contracts once 1.5 is out
    //TODO make API more like Result
    //TODO toResult() or similar
}

inline fun <R, T : R, F : KrosstalkFailure> KrosstalkCallResult<T, F>.getOrElse(onFailure: (F) -> R): R = fold({ it }, onFailure)
inline fun <R, T : R, F : KrosstalkFailure> KrosstalkCallResult<T, F>.getOrDefault(onFailure: R): R {
    if (this is KrosstalkCallResult.Success)
        return value
    else
        return onFailure
}

inline fun <R, T : R> KrosstalkCallResult<T, KrosstalkFailure>.recoverHttpError(onFailure: (KrosstalkFailure.HttpError) -> R): KrosstalkCallResult<R, KrosstalkFailure.ServerException> =
    when (this) {
        is KrosstalkCallResult.Success -> this
        is KrosstalkCallResult.Failure -> when (failure) {
            is KrosstalkFailure.HttpError -> KrosstalkCallResult.success(onFailure(failure))
            is KrosstalkFailure.ServerException -> KrosstalkCallResult.Failure(failure)
        }
    }

inline fun <R, T : R> KrosstalkCallResult<T, KrosstalkFailure>.recoverException(onFailure: (KrosstalkFailure.ServerException) -> R): KrosstalkCallResult<R, KrosstalkFailure.HttpError> =
    when (this) {
        is KrosstalkCallResult.Success -> this
        is KrosstalkCallResult.Failure -> when (failure) {
            is KrosstalkFailure.HttpError -> KrosstalkCallResult.Failure(failure)
            is KrosstalkFailure.ServerException -> KrosstalkCallResult.success(onFailure(failure))
        }
    }

inline fun <R, T : R> KrosstalkCallResult<T, KrosstalkFailure>.maybeRecoverHttpError(onFailure: (KrosstalkFailure.HttpError) -> KrosstalkCallResult<R, KrosstalkFailure.HttpError>): KrosstalkCallResult<R, KrosstalkFailure> =
    when (this) {
        is KrosstalkCallResult.Success -> this
        is KrosstalkCallResult.Failure -> when (failure) {
            is KrosstalkFailure.HttpError -> onFailure(failure)
            is KrosstalkFailure.ServerException -> KrosstalkCallResult.Failure(failure)
        }
    }

inline fun <R, T : R> KrosstalkCallResult<T, KrosstalkFailure>.maybeRecoverHttpError(
    responseCode: Int,
    result: () -> R,
): KrosstalkCallResult<R, KrosstalkFailure> =
    maybeRecoverHttpError {
        if (it.responseCode == responseCode)
            KrosstalkCallResult.success(result())
        else
            KrosstalkCallResult.failure(it)
    }

inline fun <R, T : R> KrosstalkCallResult<T, KrosstalkFailure>.recoverSomeHttpErrors(handlers: Map<Int, () -> R>): KrosstalkCallResult<R, KrosstalkFailure> =
    maybeRecoverHttpError {
        handlers[it.responseCode]?.invoke()?.let { KrosstalkCallResult.success(it) } ?: KrosstalkCallResult.failure(it)
    }

inline fun <R, T : R> KrosstalkCallResult<T, KrosstalkFailure>.recoverSomeHttpErrors(vararg handlers: Pair<Int, () -> R>): KrosstalkCallResult<R, KrosstalkFailure> =
    recoverSomeHttpErrors(handlers.toMap())

inline fun <R, T : R> KrosstalkCallResult<T, KrosstalkFailure>.maybeRecoverException(onFailure: (KrosstalkFailure.ServerException) -> KrosstalkCallResult<R, KrosstalkFailure.ServerException>): KrosstalkCallResult<R, KrosstalkFailure> =
    when (this) {
        is KrosstalkCallResult.Success -> this
        is KrosstalkCallResult.Failure -> when (failure) {
            is KrosstalkFailure.HttpError -> KrosstalkCallResult.Failure(failure)
            is KrosstalkFailure.ServerException -> onFailure(failure)
        }
    }

inline fun <T> KrosstalkCallResult<T, *>.onSuccess(handle: (T) -> Unit) {
    if (this is KrosstalkCallResult.Success)
        handle(value)
}

inline fun KrosstalkCallResult<*, *>.onFailure(handle: (KrosstalkFailure) -> Unit) {
    if (this is KrosstalkCallResult.Failure)
        handle(failure)
}

inline fun KrosstalkCallResult<*, *>.onHttpError(handle: (KrosstalkFailure.HttpError) -> Unit) {
    if (this is KrosstalkCallResult.Failure && failure is KrosstalkFailure.HttpError)
        handle(failure)
}

inline fun KrosstalkCallResult<*, *>.onException(handle: (KrosstalkFailure.ServerException) -> Unit) {
    if (this is KrosstalkCallResult.Failure && failure is KrosstalkFailure.ServerException)
        handle(failure)
}
