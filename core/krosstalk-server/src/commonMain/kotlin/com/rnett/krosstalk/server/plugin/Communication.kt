package com.rnett.krosstalk.server.plugin

import com.rnett.krosstalk.Headers
import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.KrosstalkResultHttpError
import com.rnett.krosstalk.KrosstalkServerException
import com.rnett.krosstalk.MethodDefinition
import com.rnett.krosstalk.ServerDefault
import com.rnett.krosstalk.WithHeaders
import com.rnett.krosstalk.addHeadersFrom
import com.rnett.krosstalk.endpoint.Endpoint
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

/**
 * A lambda to send a response from the server with the appropriate configuration and data.
 */
@KrosstalkPluginApi
public typealias Responder = suspend (statusCode: Int, contentType: String?, responseHeaders: Headers, responseBody: ByteArray) -> Unit

@KrosstalkPluginApi
@InternalKrosstalkApi
internal fun MethodDefinition<*>.getReturnBody(data: Any?): ByteArray = if (returnObject != null)
    ByteArray(0)
else
    serialization.serializeReturnValue(data)

//TODO make value class
@KrosstalkPluginApi
@InternalKrosstalkApi
internal class ResponseContext<K>(
    val krosstalk: K,
    val method: MethodDefinition<*>,
    val respond: Responder,
    val contentType: String,
) where K : Krosstalk, K : KrosstalkServer<*> {

    val responseHeaders = mutableMapOf<String, List<String>>()

    fun addResponseHeaders(other: Map<String, List<String>>) {
        responseHeaders addHeadersFrom other
    }

    //TODO something in plaintext for non-krosstalk servers?  JSON serialize the exception maybe.  I can just use Kotlinx here too, rather than getting the serializer
    internal suspend inline fun respondServerException(exception: KrosstalkResult.ServerException) {
        respond(
            500,
            "application/octet-stream",
            emptyMap(),
            krosstalk.serializeServerException(exception.withIncludeStackTrace(method.includeStacktrace))
        )
    }

    internal suspend inline fun respondHttpError(error: KrosstalkResult.HttpError) {
        respond(
            error.statusCode,
            "text/plain; charset=utf-8",
            emptyMap(),
            (error.message ?: "").encodeToByteArray()
        )
    }

    fun Any?.unwrapInnerHeaders(): Any? =
        if (method.innerWithHeaders) {
            this as WithHeaders<Any?>
            addResponseHeaders(this.headers)
            this.value
        } else {
            this
        }

    internal suspend inline fun respondSuccess(value: Any?) {
        val responseBody = method.getReturnBody(value.unwrapInnerHeaders())
        respond(200, contentType, responseHeaders, responseBody)
    }
}

//TODO tests for new behavior, new catch methods, catch for http errors only?

//TODO make inline
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
 * @param responder a lambda to respond to the request.
 */
@OptIn(InternalKrosstalkApi::class, ExperimentalStdlibApi::class, kotlin.contracts.ExperimentalContracts::class)
@KrosstalkPluginApi
public suspend fun <K> K.handle(
    serverUrl: String,
    method: MethodDefinition<*>,
    requestHeaders: Headers,
    urlArguments: Map<String, String>,
    requestBody: ByteArray,
    scopes: WantedScopes,
    handleException: (Throwable) -> Unit = { throw it },
    responder: Responder,
): Unit where K : Krosstalk, K : KrosstalkServer<*> {
    contract {
        callsInPlace(responder, InvocationKind.EXACTLY_ONCE)
        callsInPlace(handleException, InvocationKind.AT_MOST_ONCE)
    }
    with(ResponseContext(this, method, responder, method.contentType ?: serialization.contentType)) {
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
                is KrosstalkResultHttpError -> respondHttpError(exception.httpError)
                is KrosstalkServerException -> {
                    respondServerException(exception.exception)
                    val t = exception.exception.throwable
                    if (method.propagateServerExceptions && t != null) {
                        handleException(t)
                    }
                }
                else -> {
                    respondServerException(KrosstalkResult.ServerException(exception, true))
                    throw exception
                }
            }
            return
        } else {
            var result = caughtResult.getOrNull()

            if (method.outerWithHeaders) {
                result as WithHeaders<Any?>
                addResponseHeaders(result.headers)
                result = result.value
            }

            if (method.useExplicitResult) {
                when (val kr = result as KrosstalkResult<*>) {
                    is KrosstalkResult.Success -> respondSuccess(kr.value)
                    is KrosstalkResult.ServerException -> {
                        respondServerException(kr)

                        if (method.propagateServerExceptions && kr.throwable != null) {
                            handleException(kr.throwable!!)
                        }
                    }
                    is KrosstalkResult.HttpError -> respondHttpError(kr)
                }
            } else {
                respondSuccess(result)
            }
        }
    }
}