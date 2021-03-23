package com.rnett.krosstalk

import kotlinx.serialization.Serializable

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
    data class Success<T>(val value: T) : KrosstalkResult<T>()

    /**
     * A non-success HTTP response was gotten from the krosstalk call.
     *
     * @property responseCode The HTTP response code of the erroneous response.
     * @property message Any message that the KrosstalkClient associated with the response
     */
    @Serializable
    data class HttpError @PublishedApi internal constructor(val responseCode: Int, val message: String? = null) : KrosstalkResult<Nothing>()

    /**
     * An exception was thrown during the server side execution of the krosstalk call.
     *
     * @property message The exception's message.  Includes the stack trace if [ExplicitResult.includeStacktrace] is set to true on the method.
     */
    @Serializable
    data class Exception @PublishedApi internal constructor(val message: String) : KrosstalkResult<Nothing>()

    /**
     * Weather the result is success.
     */
    val isSuccess get() = this is Success

    /**
     * Whether the result is not a success.
     */
    val isFailure get() = !isSuccess

    /**
     * Get the value if successful, else null
     */
    val valueOrNull get() = if (this is Success) this.value else null

    /**
     * Transform the value if this is a successful request
     */
    inline fun <R> mapValue(block: (T) -> R): KrosstalkResult<R> = when (this) {
        is Success -> Success(block(value))
        is HttpError -> this
        is Exception -> this
    }


    /**
     * Transform the http error message if this is a http error
     */
    inline fun <R> mapHttpErrorMessage(block: (Int, String?) -> String?): KrosstalkResult<T> = when (this) {
        is Success -> this
        is HttpError -> HttpError(responseCode, block(responseCode, message))
        is Exception -> this
    }

    /**
     * Transform the exception message if this is an exception
     */
    inline fun <R> mapExceptionMessage(block: (String) -> String): KrosstalkResult<T> = when (this) {
        is Success -> this
        is HttpError -> this
        is Exception -> Exception(block(message))
    }

    companion object {
        /**
         * Shortcut to create a success
         */
        inline operator fun <T> invoke(value: T) = Success(value)
    }

}


/**
 * Get the value if success, get a value from the http error if not.
 *
 * Throws an exception if this is [KrosstalkResult.Exception].
 */
inline fun <T> KrosstalkResult<T>.successOrHttpError(onHttpError: (KrosstalkResult.HttpError) -> T): T = when (this) {
    is KrosstalkResult.Success -> value
    is KrosstalkResult.HttpError -> onHttpError(this)
    is KrosstalkResult.Exception -> throw error("Expected successful result or a HTTP error, got an exception: $message")
}

/**
 * Get the value if success, get a value from the http error code if not.
 *
 * Throws an exception if this is [KrosstalkResult.Exception], or if it is a http error but the response code is not in the map.
 */
inline fun <T> KrosstalkResult<T>.successOrResponseCode(mappedErrors: Map<Int, T>): T = successOrHttpError {
    mappedErrors[it.responseCode] ?: error("Unexpected response code: ${it.responseCode} with message ${it.message}")
}

/**
 * Get the value if success, get a value from the http error code if not.
 *
 * Throws an exception if this is [KrosstalkResult.Exception], or if it is a http error but the response code is not in the map.
 */
inline fun <T> KrosstalkResult<T>.successOrResponseCode(vararg mappedErrors: Pair<Int, T>): T = successOrResponseCode(mappedErrors.toMap())
