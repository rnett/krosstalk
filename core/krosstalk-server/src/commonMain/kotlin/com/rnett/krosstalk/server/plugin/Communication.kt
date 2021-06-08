package com.rnett.krosstalk.server.plugin

import com.rnett.krosstalk.Headers
import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KROSSTALK_THROW_EXCEPTION_HEADER_NAME
import com.rnett.krosstalk.KROSSTALK_UNCAUGHT_EXCEPTION_HEADER_NAME
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.MethodDefinition
import com.rnett.krosstalk.ServerDefault
import com.rnett.krosstalk.WithHeaders
import com.rnett.krosstalk.addHeaders
import com.rnett.krosstalk.endpoint.Endpoint
import com.rnett.krosstalk.mutableHeadersOf
import com.rnett.krosstalk.result.KrosstalkHttpError
import com.rnett.krosstalk.result.KrosstalkResult
import com.rnett.krosstalk.result.KrosstalkServerException
import com.rnett.krosstalk.serializeServerException
import com.rnett.krosstalk.server.KrosstalkServer
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * A Krosstalk server handler.
 * Should help you set up receiver endpoints for all Krosstalk methods ([Krosstalk.methods]), which should each call [handle] for their method.
 * No behavior is defined here as what is necessary to do that, and where and how you will want to call it, will vary widely depending on which server you are using.
 *
 * You will almost certainly want to use [Endpoint.resolve] to handle endpoint templates.  [Endpoint.allResolvePaths] is available as well, but
 * is not yet well supported, you would have to write your own resolution code or use your server implementation's.
 *
 * See the `krosstalk-ktor-server` artifact for an example server implementation using Ktor.
 */
@KrosstalkPluginApi
public interface ServerHandler<S : ServerScope<*>>

//TODO make value class
/**
 * A response to send from the server.  Should not set content type if [contentType] is null.
 */
@KrosstalkPluginApi
public class KrosstalkResponse(public val statusCode: Int, public val contentType: String?, public val responseHeaders: Headers, public val responseBody: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as KrosstalkResponse

        if (statusCode != other.statusCode) return false
        if (contentType != other.contentType) return false
        if (responseHeaders != other.responseHeaders) return false
        if (!responseBody.contentEquals(other.responseBody)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = statusCode
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + responseHeaders.hashCode()
        result = 31 * result + responseBody.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "KrosstalkResponse(statusCode=$statusCode, contentType=$contentType, responseHeaders=$responseHeaders, responseBody=${responseBody.contentToString()})"
    }
}

@KrosstalkPluginApi
@InternalKrosstalkApi
@PublishedApi
internal fun MethodDefinition<*>.getReturnBody(data: Any?): ByteArray = if (returnObject != null)
    ByteArray(0)
else
    serialization.serializeReturnValue(data)

//TODO make value class
@KrosstalkPluginApi
@InternalKrosstalkApi
@PublishedApi
internal class ResponseContext<K>(
    val krosstalk: K,
    val method: MethodDefinition<*>,
    val contentType: String,
) where K : Krosstalk, K : KrosstalkServer<*> {

    val responseHeaders = mutableHeadersOf()

    @PublishedApi
    internal inline fun serverExceptionResponse(
        exception: KrosstalkResult.ServerException,
        throwing: Boolean = false,
        uncaught: Boolean = false
    ): KrosstalkResponse = KrosstalkResponse(
        500,
        "text/plain; charset=utf-8",
        responseHeaders.addHeaders {
            if (throwing)
                this[KROSSTALK_THROW_EXCEPTION_HEADER_NAME] = "true"
            if (uncaught)
                this[KROSSTALK_UNCAUGHT_EXCEPTION_HEADER_NAME] = "true"
        },
        serializeServerException(exception.withIncludeStackTrace(method.includeStacktrace))
    )

    @PublishedApi
    internal inline fun httpErrorResponse(error: KrosstalkResult.HttpError, throwing: Boolean = false): KrosstalkResponse =
        KrosstalkResponse(
            error.statusCode,
            "text/plain; charset=utf-8",
            responseHeaders.addHeaders {
                if (throwing)
                    this[KROSSTALK_THROW_EXCEPTION_HEADER_NAME] = "true"
            },
            (error.message ?: "").encodeToByteArray()
        )

    fun Any?.unwrapInnerHeaders(): Any? =
        if (method.innerWithHeaders) {
            this as WithHeaders<Any?>
            responseHeaders += this.headers
            this.value
        } else {
            this
        }

    @PublishedApi
    internal inline fun successResponse(value: Any?): KrosstalkResponse {
        val responseBody = method.getReturnBody(value.unwrapInnerHeaders())
        return KrosstalkResponse(200, contentType, responseHeaders, responseBody)
    }
}

/**
 * Helper method for server side to handle a request for a Krosstalk [method].
 *
 * @param serverUrl the base url of the server, **not including the method's endpoint**.
 * @param method method to call
 * @param requestHeaders the headers of the request
 * @param urlArguments the arguments gotten from the URL (probably using the endpoint's resolver)
 * @param requestBody the body of the request
 * @param scopes the scopes gotten from the request.  Missing required scopes will be handled here.
 * @param handleException how to handle a server exception, if requested.  Should log if possible, throw if not.
 * @param sendResponse a lambda to respond to the request.
 */
@OptIn(InternalKrosstalkApi::class, ExperimentalStdlibApi::class, kotlin.contracts.ExperimentalContracts::class)
@KrosstalkPluginApi
public suspend inline fun <K> K.handle(
    serverUrl: String,
    method: MethodDefinition<*>,
    requestHeaders: Headers,
    urlArguments: Map<String, String>,
    requestBody: ByteArray,
    scopes: WantedScopes,
    handleException: (Throwable) -> Unit = { throw it },
    sendResponse: (KrosstalkResponse) -> Unit,
): Unit where K : Krosstalk, K : KrosstalkServer<*> {
    contract {
        callsInPlace(sendResponse, InvocationKind.EXACTLY_ONCE)
        callsInPlace(handleException, InvocationKind.AT_MOST_ONCE)
    }
    with(ResponseContext(this, method, method.contentType ?: serialization.contentType)) {
        val wantedScopes = scopes.toImmutable()

        val arguments: Map<String, Any?> = buildMap() {
            putAll(method.objectParameters)

            urlArguments.forEach { (key, value) ->
                put(key, method.serialization.deserializeUrlArg(key, value))
            }

            if (requestBody.isNotEmpty())
                putAll(method.serialization.deserializeBodyArguments(requestBody))

            if (method.requestHeadersParam != null)
                put(method.requestHeadersParam!!, requestHeaders)

            if (method.serverUrlParam != null)
                put(method.serverUrlParam!!, serverUrl)
        }

        val caughtResult = kotlin.runCatching {
            method.call(arguments.mapValues {
                if (it.key in method.serverDefaultParameters)
                    ServerDefault { it.value }
                else
                    it.value
            }, wantedScopes)
        }

        if (caughtResult.isFailure) {
            when (val exception = caughtResult.exceptionOrNull()!!) {
                is KrosstalkHttpError -> sendResponse(httpErrorResponse(exception.httpError, true))
                is KrosstalkServerException -> {
                    sendResponse(serverExceptionResponse(exception.exception, true))
                    val t = exception.exception.throwable
                    if (method.propagateServerExceptions && t != null) {
                        handleException(t)
                    }
                }
                else -> {
                    sendResponse(serverExceptionResponse(KrosstalkResult.ServerException(exception, true), throwing = true, uncaught = true))
                    throw exception
                }
            }
            return
        } else {
            var result = caughtResult.getOrNull()

            if (method.outerWithHeaders) {
                result as WithHeaders<Any?>
                responseHeaders += result.headers
                result = result.value
            }

            if (method.useExplicitResult) {
                when (val kr = result as KrosstalkResult<*>) {
                    is KrosstalkResult.Success -> sendResponse(successResponse(kr.value))
                    is KrosstalkResult.ServerException -> {
                        sendResponse(serverExceptionResponse(kr))

                        if (method.propagateServerExceptions && kr.throwable != null) {
                            handleException(kr.throwable!!)
                        }
                    }
                    is KrosstalkResult.HttpError -> sendResponse(httpErrorResponse(kr))
                }
            } else {
                sendResponse(successResponse(result))
            }
        }
    }
}