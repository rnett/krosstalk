package com.rnett.krosstalk.server

import com.rnett.krosstalk.Headers
import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.MethodDefinition
import com.rnett.krosstalk.ServerDefault
import com.rnett.krosstalk.WithHeaders
import com.rnett.krosstalk.addHeadersFrom
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


//TODO update docs
/**
 * A Krosstalk server handler.
 * Should help you set up receiver endpoints for all Krosstalk methods.
 * No behavior is defined here as what is necessary to do that, and where and how you will want to call it, will vary widely depending on which server you are using.
 *
 * Will almost certainly want to use [fillWithStaticAndAdjustParameters], [fillWithStatic], or [splitEndpoint] to handle endpoint templates.
 */
@KrosstalkPluginApi
interface ServerHandler<S : ServerScope<*>>

typealias Responder = suspend (statusCode: Int, contentType: String?, responseHeaders: Headers, data: ByteArray) -> Unit

@InternalKrosstalkApi
fun MethodDefinition<*>.getReturnBody(data: Any?): ByteArray = if (returnObject != null)
    ByteArray(0)
else
    serialization.serializeReturnValue(data)

/**
 * Helper method for server side to handle a method [methodName] with the body data [body].
 *
 * @param serverUrl the base url of the server, **not including the method's endpoint**.
 * @param method method to call
 * @param requestHeaders the headers of the request
 * @param urlArguments the arguments gotten from the URL (probably using the endpoint's resolver)
 * @param body the body
 * @param scopes the scopes gotten from the request.  Missing required scopes will be handled here.
 * @param handleException how to handle a server exception, if requested.  Should log if possible, throw if not.
 * @param responder a lambda to respond to the request.
 */
@OptIn(InternalKrosstalkApi::class, ExperimentalStdlibApi::class)
@KrosstalkPluginApi
suspend fun <K> K.handle(
    serverUrl: String,
    method: MethodDefinition<*>,
    requestHeaders: Headers,
    urlArguments: Map<String, String>,
    body: ByteArray,
    scopes: WantedScopes,
    handleException: (Throwable) -> Unit = { throw it },
    responder: Responder,
): Unit where K : Krosstalk, K : KrosstalkServer<*> {
    contract {
        callsInPlace(responder, InvocationKind.EXACTLY_ONCE)
        callsInPlace(handleException, InvocationKind.AT_MOST_ONCE)
    }
    val wantedScopes = scopes.toImmutable()
    val contentType = method.contentType ?: serialization.contentType

    val arguments: Map<String, Any?> = buildMap() {
        putAll(method.objectParameters)

        urlArguments.forEach { (key, value) ->
            put(key, method.serialization.deserializeUrlArg(key, value))
        }

        if (body.isNotEmpty())
            putAll(method.serialization.deserializeBodyArguments(body))

        if (method.requestHeadersParam != null)
            put(method.requestHeadersParam!!, requestHeaders)

        if (method.serverUrlParam != null)
            put(method.serverUrlParam!!, serverUrl)
    }

    val givenHeaders = mutableMapOf<String, List<String>>()

    var result = method.call(arguments.mapValues {
        if (it.key in method.serverDefaultParameters)
            ServerDefault { it.value }
        else
            it.value
    }, wantedScopes)

    if (method.outerWithHeaders) {
        result as WithHeaders<Any?>
        givenHeaders addHeadersFrom result.headers
        result = result.value
    }

    val exception = if (method.propagateServerExceptions && result is KrosstalkResult.ServerException) {
        result.throwable
    } else
        null

    fun Any?.unwrapHeaders(): Any? =
        if (method.innerWithHeaders) {
            this as WithHeaders<Any?>
            givenHeaders addHeadersFrom this.headers
            this.value
        } else {
            this
        }

    if (method.useExplicitResult) {
        when (val kr = result as KrosstalkResult<*>) {
            is KrosstalkResult.Success -> {
                val responseBody = method.getReturnBody(kr.value.unwrapHeaders())
                responder(200, contentType, givenHeaders, responseBody)
            }
            is KrosstalkResult.ServerException -> {
                //TODO something in plaintext for non-krosstalk servers?  JSON serialize the exception maybe.  I can just use Kotlinx here too, rather than getting the serializer
                responder(
                    500,
                    "application/octet-stream",
                    emptyMap(),
                    serializeServerException(kr.withIncludeStackTrace(method.includeStacktrace))
                )
            }
            is KrosstalkResult.HttpError -> {
                responder(
                    kr.statusCode,
                    "text/plain; charset=utf-8",
                    emptyMap(),
                    (kr.message ?: "").encodeToByteArray()
                )
            }
        }
    } else {
        val responseBody = method.getReturnBody(result.unwrapHeaders())
        responder(200, contentType, givenHeaders, responseBody)
    }

    if (exception != null) {
        handleException(exception)
    }
}