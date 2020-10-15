package com.rnett.krosstalk

import com.rnett.krosstalk.annotations.ExplicitResult
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

/**
 * The response to a Krosstalk request.
 */
sealed class InternalKrosstalkResponse {
    /**
     * The HTTP response code.
     */
    abstract val responseCode: Int

    /**
     * A Successful request, returning the serialized return value.
     */
    data class Success(override val responseCode: Int, val data: ByteArray) : InternalKrosstalkResponse() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false

            if (responseCode != other.responseCode) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = responseCode
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    /**
     * A failed request.  Can throw a custom error using [error], will throw [KrosstalkException.CallFailure] if [error] is null.
     */
    data class Failure(override val responseCode: Int, val error: (suspend (methodName: String) -> Nothing)? = null) : InternalKrosstalkResponse()
}

/**
 * A Krosstalk client handler.  Capable of sending krosstalk requests.
 */
interface ClientHandler<C : ClientScope<*>> {

    /**
     * The url of the server to send requests to.
     */
    val serverUrl: String


    /**
     * Helper function to concatenate the server url and the endpoint
     */
    fun requestUrl(endpoint: String) = "${serverUrl.trimEnd('/')}/${endpoint.trimStart('/')}"

    /**
     * Helper function to throw a [KrosstalkException.CallFailure].
     */
    fun callFailedException(
        methodName: String,
        httpStatusCode: Int,
        message: String = "Krosstalk method $methodName failed with HTTP status code $httpStatusCode"
    ): Nothing = throw KrosstalkException.CallFailure(methodName, httpStatusCode, message)

    /**
     * Send a krosstalk request to the server.
     * Should generally make a [httpMethod] request to `$serverUrl/$endpoint` (which can be gotten using [requestUrl]), but can change this if necessary.
     *
     * @param endpoint The endpoint to send it to.
     * @param httpMethod The HTTP method (i.e. GET, POST) to use.
     * @param body The body to send, or null if a body shouldn't be sent.
     * @param scopes The scopes to apply to the request.
     * @return The result of the request.
     */
    suspend fun sendKrosstalkRequest(
        endpoint: String,
        httpMethod: String,
        body: ByteArray?,
        scopes: List<ActiveScope<*, C>>
    ): InternalKrosstalkResponse
}

/**
 * A Krosstalk server handler.
 * Should help you set up receiver endpoints for all Krosstalk methods.
 * No behavior is defined here as what is necessary to do that, and where and how you will want to call it, will vary widely depending on which server you are using.
 *
 * Will almost certainly want to use [fillWithStaticAndAdjustParameters], [fillWithStatic], or [splitEndpoint] to handle endpoint templates.
 */
interface ServerHandler<S : ServerScope>

//fun ServerHandler<*>.fillEndpoint(endpointTemplate: String, methodName: String, )

/**
 * Placeholder for a Krosstalk client side method.  Will be replaced by the compiler plugin.
 */
suspend fun krosstalkCall(): Nothing =
    throw KrosstalkException.CompilerError("Should have been replaced with a krosstalk call during compilation")

@Suppress("unused")
@PublishedApi
internal suspend inline fun <T, K, reified C : ClientScope<*>> K.call(methodName: String, arguments: Map<String, *>): T
        where K : KrosstalkClient<C>, K : Krosstalk {
    val method = requiredMethod(methodName)
    val keptArgs = if (method.leaveOutArguments == null) arguments else arguments.filterKeys { it !in method.leaveOutArguments }
    val serializedArgs = serialization.serializeByteArguments(keptArgs, method.serializers)

    val usedScopes = mutableListOf<ActiveScope<*, *>>()

    val missingRequires = mutableSetOf<String>()

    method.requiredScopes.forEach {
        val holder = _scopes.getValue(it)

        if (holder !in activeScopes)
            missingRequires += it
        else
            usedScopes += activeScopes.getValue(holder)
    }

    if (missingRequires.isNotEmpty())
        throw KrosstalkException.MissingScope(methodName, missingRequires, activeScopes.map { it.key.name }.toSet())

    method.optionalScopes.forEach {
        activeScopes[_scopes.getValue(it)]?.let(usedScopes::add)
    }

    val result = client.sendKrosstalkRequest(
        method.endpoint.fillWithArgs(methodName, this.endpointPrefix, arguments),
        method.httpMethod,
        if (keptArgs.isEmpty() && method.leaveOutArguments != null) null else serializedArgs,
        usedScopes.map {
            if (it.scope !is C) throw KrosstalkException.CompilerError("Scope ${it.scope} was not of required type ${C::class}.")
            it as ActiveScope<*, C>
        })

    @Suppress("UNCHECKED_CAST") // checked in compiler plugin
    if (result.responseCode in method.nullOnResponseCodes)
        return null as T

    if (method.useExplicitResult) {
        //TODO ensure T is KrosstalkResult
        return when (result) {
            // will be a success or exception
            is InternalKrosstalkResponse.Success -> method.serializers.resultSerializer.deserializeFromBytes(result.data) as T
            is InternalKrosstalkResponse.Failure -> {
                try {
                    result.error?.let { it(methodName) }
                        ?: client.callFailedException(methodName, result.responseCode)
                } catch (e: Throwable) {
                    KrosstalkResult.HttpError(result.responseCode, e.message)
                } as T
            }
        }
    } else {
        return when (result) {
            is InternalKrosstalkResponse.Success -> method.serializers.resultSerializer.deserializeFromBytes(
                result.data
            ) as T
            is InternalKrosstalkResponse.Failure -> result.error?.let { it(methodName) }
                ?: client.callFailedException(methodName, result.responseCode)
        }
    }
}

/**
 * Helper method for server side to handle a method [methodName] with the body data [data].
 *
 * @return The data that should be sent as a response.
 */
suspend fun <K> K.handle(methodName: String, data: ByteArray): ByteArray where K : Krosstalk, K : KrosstalkServer<*> {
    val method = requiredMethod(methodName)

    if (method.leaveOutArguments != null)
        throw KrosstalkException.ClientOnlyAnnotationOnServer("You somehow set MinimizeBody or EmptyBody on a call with a krosstalk server.  It should cause a compiler error, this is a bug.")

    val arguments = serialization.deserializeByteArguments(data, method.serializers)


    val result = if (method.useExplicitResult) {
        try {
            method.call(arguments)
        } catch (e: Throwable) {
            if (method.includeStacktrace)
                KrosstalkResult.Exception(e.stackTraceToString())
            else
                KrosstalkResult.Exception(e.toString())
        }
    } else {
        method.call(arguments)
    }
    val resultSerializer = method.serializers.resultSerializer as Serializer<Any?, *>
    return resultSerializer.serializeToBytes(result)
}