package com.rnett.krosstalk.client

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkException
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.httpEncode


/**
 * The response to a Krosstalk request.
 */
@KrosstalkPluginApi
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
    data class Failure(override val responseCode: Int, val error: (suspend (methodName: String) -> Nothing)? = null) :
        InternalKrosstalkResponse()
}

/**
 * Helper function to throw a [KrosstalkException.CallFailure].
 */
@OptIn(InternalKrosstalkApi::class)
@KrosstalkPluginApi
fun ClientHandler<*>.callFailedException(
    methodName: String,
    httpStatusCode: Int,
    message: String = "Krosstalk method $methodName failed with HTTP status code $httpStatusCode",
): Nothing = throw KrosstalkException.CallFailure(methodName, httpStatusCode, message)

/**
 * A Krosstalk client handler.  Capable of sending krosstalk requests.
 */
@KrosstalkPluginApi
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
        scopes: List<AppliedClientScope<C, *>>,
    ): InternalKrosstalkResponse
}


//fun ServerHandler<*>.fillEndpoint(endpointTemplate: String, methodName: String, )

/**
 * Placeholder for a Krosstalk client side method.  Will be replaced by the compiler plugin.
 */
@OptIn(InternalKrosstalkApi::class)
suspend fun krosstalkCall(): Nothing =
    throw KrosstalkException.CompilerError("Should have been replaced with a krosstalk call during compilation")


@OptIn(InternalKrosstalkApi::class, KrosstalkPluginApi::class)
@Suppress("unused")
@PublishedApi
internal suspend inline fun <T, K, reified C : ClientScope<*>> K.call(
    methodName: String,
    arguments: Map<String, *>,
    scopes: List<AppliedClientScope<C, *>>,
): T
        where K : KrosstalkClient<C>, K : Krosstalk {
    val method = requiredMethod(methodName)

    val bodyArguments = method.bodyArguments(arguments)
    println("Body Arguments: ${bodyArguments.keys}")
    val serializedBody = method.serializers.transformedParamSerializers.serializeArgumentsToBytes(bodyArguments)

    val serializedUrlArgs = method.urlArguments(arguments)
        .mapValues {
            method.serializers.transformedParamSerializers.serializeArgumentToString(it.key, it.value).httpEncode()
        }


    val result = client.sendKrosstalkRequest(
        method.endpoint.fillWithArgs(
            methodName,
            serializedUrlArgs,
            arguments.filter { it.value != null }.keys
        ),
        method.httpMethod,
        if (!method.hasBodyArguments(arguments)) null else serializedBody,
        scopes
    )

    @Suppress("UNCHECKED_CAST") // checked in compiler plugin
    if (result.responseCode in method.nullOnResponseCodes)
        return null as T

    if (method.useExplicitResult) {
        //TODO ensure T is KrosstalkResult (is done in compiler plugin too, but wouldn't hurt)
        return when (result) {
            // will be a success or exception
            is InternalKrosstalkResponse.Success -> method.serializers.transformedResultSerializer.deserializeFromBytes(
                result.data
            ) as T
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
            is InternalKrosstalkResponse.Success -> method.serializers.transformedResultSerializer.deserializeFromBytes(
                result.data
            ) as T
            is InternalKrosstalkResponse.Failure -> result.error?.let { it(methodName) }
                ?: client.callFailedException(methodName, result.responseCode)
        }
    }
}
