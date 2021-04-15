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
import com.rnett.krosstalk.httpStatusCodes
import com.rnett.krosstalk.isNone
import kotlin.reflect.KClass


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
     * Send a krosstalk request to the server.
     *
     * @param url The endpoint to send it to.
     * @param httpMethod The HTTP method (i.e. GET, POST) to use.
     * @param contentType the content type of the body
     * @param additionalHeaders additional headers to set on the request
     * @param body The body to send, or null if a body shouldn't be sent.
     * @param scopes The scopes to apply to the request.
     * @return The result of the request.
     */
    suspend fun sendKrosstalkRequest(
        url: String,
        httpMethod: String,
        contentType: String,
        additionalHeaders: Headers,
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

@InternalKrosstalkApi
class ServerDefaultInEndpointException(val methodName: String, val parameter: String) :
    KrosstalkException("Parameter \"$parameter\" for method \"$methodName\" was an unrealized ServerDefault, but was used in the endpoint.")


/**
 * A Krosstalk call failed.
 */
@OptIn(InternalKrosstalkApi::class)
open class CallFailureException @InternalKrosstalkApi constructor(
    val methodName: String,
    val httpStatusCode: Int,
    val responseMessage: String?,
    message: String = buildString {
        append("Krosstalk method $methodName failed with HTTP status code $httpStatusCode")
        if (httpStatusCode in httpStatusCodes)
            append(": ${httpStatusCodes[httpStatusCode]}")

        if (responseMessage != null)
            append(" and response message: $responseMessage")

    },
) : KrosstalkException(message)

@OptIn(InternalKrosstalkApi::class)
class WrongHeadersTypeException @InternalKrosstalkApi constructor(val methodName: String, val type: KClass<*>) : KrosstalkException(
    "Invalid type for request headers param: Map<String, List<String>> is required, got $type."
)

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

    val requestHeaders = if (method.requestHeadersParam != null) {
        rawArguments.getValue(method.requestHeadersParam!!)!!.let { it as? Headers ?: throw WrongHeadersTypeException(methodName, it::class) }
    } else null

    val serverUrl = method.serverUrlParam?.let { rawArguments.getValue(it) as String? } ?: this.serverUrl

    val arguments = rawArguments.filterValues {
        !(it is ServerDefault<*> && it.isNone())
    }.filterKeys {
        it != method.requestHeadersParam && it != method.serverUrlParam
    }.mapValues {
        if (it.value is ServerDefault<*>)
            (it.value as ServerDefault<*>).value
        else
            it.value
    }

    val (endpoint, usedInUrl) = method.endpoint.fillWithArgs(
        methodName,
        rawArguments.keys,
        arguments.filter { it.value != null }.keys
    ) {
        if (rawArguments[it].let { it is ServerDefault<*> && it.isNone() })
            throw ServerDefaultInEndpointException(methodName, it)

        method.serialization.serializeUrlArg(it, arguments[it])
    }

    val bodyArguments = arguments
        .filterNot { it.value == null && it.key in method.optionalParameters }.minus(method.objectParameters.keys).minus(usedInUrl)

    val serializedBody = method.serialization.serializeBodyArguments(bodyArguments)

    val result = client.sendKrosstalkRequest(
        serverUrl.trimEnd('/') + "/" + endpoint.trimStart('/'),
        method.httpMethod,
        method.contentType ?: serialization.contentType,
        requestHeaders ?: emptyMap(),
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
                    KrosstalkResult.HttpError(result.statusCode, result.stringData)
                }
            } else {
                KrosstalkResult.HttpError(result.statusCode, result.stringData)
            }
        }
    } else {
        if (result.isSuccess()) {
            method.getReturnValue(result.data).withHeadersIf(method.innerWithHeaders, result.headers)
        } else {
            throw CallFailureException(methodName, result.statusCode, result.stringData)
        }
    }.withHeadersIf(method.outerWithHeaders, result.headers) as T
}
