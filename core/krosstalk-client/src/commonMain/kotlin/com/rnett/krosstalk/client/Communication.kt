package com.rnett.krosstalk.client

import com.rnett.krosstalk.Headers
import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkException
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.MethodDefinition
import com.rnett.krosstalk.ServerDefault
import com.rnett.krosstalk.WithHeaders
import com.rnett.krosstalk.isNone


/**
 * The response to a Krosstalk request.
 */
@KrosstalkPluginApi
class InternalKrosstalkResponse(val statusCode: Int, val headers: Headers, val data: ByteArray, stringData: () -> String?) {
    val stringData by lazy {
        if (data.isEmpty())
            null
        else
            stringData()
    }

    @PublishedApi
    internal fun isSuccess() = statusCode in 200..299
}

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
        contentType: String,
        body: ByteArray?,
        scopes: List<AppliedClientScope<C, *>>,
    ): InternalKrosstalkResponse

    fun getStatusCodeName(httpStatusCode: Int): String?
}


//fun ServerHandler<*>.fillEndpoint(endpointTemplate: String, methodName: String, )

/**
 * Placeholder for a Krosstalk client side method.  Will be replaced by the compiler plugin.
 */
@OptIn(InternalKrosstalkApi::class)
suspend fun krosstalkCall(): Nothing =
    throw KrosstalkException.CompilerError("Should have been replaced with a krosstalk call during compilation")

@InternalKrosstalkApi
class NoneInUrlException(val methodName: String, val parameter: String) :
    KrosstalkException("Parameter \"$parameter\" for method \"$methodName\" was an unrealized ServerDefault, but was used in URL.")


/**
 * A Krosstalk call failed.
 */
@OptIn(InternalKrosstalkApi::class)
open class CallFailureException @InternalKrosstalkApi constructor(
    val methodName: String,
    val httpStatusCode: Int,
    val httpStatusCodeMessage: String?,
    val responseMessage: String?,
    message: String = buildString {
        append("Krosstalk method $methodName failed with HTTP status code $httpStatusCode")
        if (httpStatusCodeMessage != null)
            append(": $httpStatusCodeMessage")

        if (responseMessage != null)
            append(" and response message: $responseMessage")

    },
) : KrosstalkException(message)

@InternalKrosstalkApi
@PublishedApi
internal fun <T> MethodDefinition<T>.getReturnValue(data: ByteArray): T = if (returnObject != null) {
    returnObject as T
} else {
    serialization.deserializeReturnValue(data) as T
}

@PublishedApi
internal fun Any?.withHeadersIf(withHeaders: Boolean, headers: Headers) = if (withHeaders) WithHeaders(this, headers) else this

@OptIn(InternalKrosstalkApi::class, KrosstalkPluginApi::class, ExperimentalStdlibApi::class)
@Suppress("unused")
@PublishedApi
internal suspend inline fun <T, K, reified C : ClientScope<*>> K.call(
    methodName: String,
    rawArguments: Map<String, *>,
    scopes: List<AppliedClientScope<C, *>>,
): T where K : KrosstalkClient<C>, K : Krosstalk {

    val method = requiredMethod(methodName)

    val arguments = rawArguments.filterValues {
        !(it is ServerDefault<*> && it.isNone())
    }.mapValues {
        if (it.value is ServerDefault<*>)
            (it.value as ServerDefault<*>).value
        else
            it.value
    }

    val (url, usedInUrl) = method.endpoint.fillWithArgs(
        methodName,
        rawArguments.keys,
        arguments.filter { it.value != null }.keys
    ) {
        if (rawArguments[it].let { it is ServerDefault<*> && it.isNone() })
            throw NoneInUrlException(methodName, it)

        method.serialization.serializeUrlArg(it, arguments[it])
    }

    val bodyArguments = arguments
        .filterNot { it.value == null && it.key in method.optionalParameters }.minus(method.objectParameters.keys).minus(usedInUrl)

    val serializedBody = method.serialization.serializeBodyArguments(bodyArguments)

    val result = client.sendKrosstalkRequest(
        url,
        method.httpMethod,
        method.contentType ?: serialization.contentType,
        if (bodyArguments.isEmpty()) null else serializedBody,
        scopes
    )

    return if (method.useExplicitResult) {
        if (result.isSuccess()) {
            KrosstalkResult.Success(method.getReturnValue(result.data).withHeadersIf(method.innerWithHeaders, result.headers))
        } else {
            if (result.statusCode == 500) {
                try {
                    deserializeServerException(result.data)
                } catch (t: Throwable) {
                    KrosstalkResult.HttpError(result.statusCode, client.getStatusCodeName(result.statusCode), result.stringData)
                }
            } else {
                KrosstalkResult.HttpError(result.statusCode, client.getStatusCodeName(result.statusCode), result.stringData)
            }
        }
    } else {
        if (result.isSuccess()) {
            method.getReturnValue(result.data).withHeadersIf(method.innerWithHeaders, result.headers)
        } else {
            throw CallFailureException(methodName, result.statusCode, client.getStatusCodeName(result.statusCode), result.stringData)
        }
    }.withHeadersIf(method.outerWithHeaders, result.headers) as T
}
