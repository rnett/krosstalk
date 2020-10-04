package com.rnett.krosstalk

/**
 * The response to a Krosstalk request.
 */
sealed class KrosstalkResponse {
    /**
     * The HTTP response code.
     */
    abstract val responseCode: Int

    /**
     * A Successful request, returning the serialized return value.
     */
    data class Success(override val responseCode: Int, val data: ByteArray) : KrosstalkResponse() {
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
    data class Failure(override val responseCode: Int, val error: (suspend (methodName: String) -> Nothing)? = null) : KrosstalkResponse()
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
    ): KrosstalkResponse
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
    val keptArgs = arguments.filterKeys { it !in method.leaveOutArguments }
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
        if (keptArgs.isEmpty()) null else serializedArgs,
        usedScopes.map {
            if (it.scope !is C) throw KrosstalkException.CompilerError("Scope ${it.scope} was not of required type ${C::class}.")
            it as ActiveScope<*, C>
        })

    @Suppress("UNCHECKED_CAST") // checked in compiler plugin
    if (result.responseCode in method.nullOnResponseCodes)
        return null as T

    return when (result) {
        is KrosstalkResponse.Success -> method.serializers.resultSerializer.deserializeFromBytes(result.data) as T
        is KrosstalkResponse.Failure -> result.error?.let { it(methodName) }
            ?: client.callFailedException(methodName, result.responseCode)
    }
}

/**
 * Helper method for server side to handle a method [methodName] with the body data [data].
 *
 * @return The data that should be sent as a response.
 */
suspend fun <K> K.handle(methodName: String, data: ByteArray): ByteArray where K : Krosstalk, K : KrosstalkServer<*> {
    val method = requiredMethod(methodName)

    if (method.leaveOutArguments.isNotEmpty())
        throw KrosstalkException.ClientOnlyAnnotationOnServer("You somehow set MinimizeBody or EmptyBody on a call with a krosstalk server.  It should cause a compiler error, this is a bug.")

    val arguments = serialization.deserializeByteArguments(data, method.serializers)

    val result = method.call(arguments)
    val resultSerializer = method.serializers.resultSerializer as Serializer<Any?, *>
    return resultSerializer.serializeToBytes(result)
}