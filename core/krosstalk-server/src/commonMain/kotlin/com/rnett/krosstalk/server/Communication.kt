package com.rnett.krosstalk.server

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.httpDecode
import com.rnett.krosstalk.serialization.getMethodSerializer
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * A Krosstalk server handler.
 * Should help you set up receiver endpoints for all Krosstalk methods.
 * No behavior is defined here as what is necessary to do that, and where and how you will want to call it, will vary widely depending on which server you are using.
 *
 * Will almost certainly want to use [fillWithStaticAndAdjustParameters], [fillWithStatic], or [splitEndpoint] to handle endpoint templates.
 */
@KrosstalkPluginApi
interface ServerHandler<S : ServerScope<*>> {

    fun getStatusCodeName(httpStatusCode: Int): String?
}

data class KrosstalkResponse(val data: ByteArray, val exception: Throwable?) {
    fun throwException(): Nothing? {
        if (exception != null)
            throw exception
        return null
    }
}

typealias Responder = suspend (statusCode: Int, contentType: String?, data: ByteArray) -> Unit

/**
 * Helper method for server side to handle a method [methodName] with the body data [data].
 *
 * @return The data that should be sent as a response.
 */
@OptIn(InternalKrosstalkApi::class)
@KrosstalkPluginApi
suspend fun <K> K.handle(
    methodName: String,
    urlArguments: Map<String, String>,
    data: ByteArray,
    scopes: WantedScopes,
    handleException: (Throwable) -> Unit = { throw it },
    responder: Responder,
): Unit where K : Krosstalk, K : KrosstalkServer<*> {
    contract {
        callsInPlace(responder, InvocationKind.EXACTLY_ONCE)
        callsInPlace(handleException, InvocationKind.AT_MOST_ONCE)
    }
    val wantedScopes = scopes.toImmutable()
    val method = requiredMethod(methodName)
    val contentType = "application/*" //TODO set in KrosstalkEndpoint

    val arguments = if (data.isNotEmpty())
        method.serializers.transformedParamSerializers.deserializeArgumentsFromBytes(data).toMutableMap()
    else
        mutableMapOf()

    if (method.minimizeBody) {
        val endpointArguments: Map<String, Any?> = urlArguments.mapValues {
            method.serializers.transformedParamSerializers.deserializeArgumentFromString(it.key, it.value.httpDecode())
        }

        arguments += endpointArguments
    }

    val result = method.call(arguments, wantedScopes)

    val exception = if (method.propagateServerExceptions && result is KrosstalkResult.ServerException) {
        result.throwable
    } else
        null

    if (method.useExplicitResult) {
        when (val kr = result as KrosstalkResult<*>) {
            is KrosstalkResult.Success -> {
                responder(200, contentType, method.serializers.transformedResultSerializer.serializeToBytes(kr.value))
            }
            is KrosstalkResult.ServerException -> {
                responder(500, "application/octet-stream", serialization.getMethodSerializer<KrosstalkResult.ServerException>().serializeToBytes(kr))
            }
            is KrosstalkResult.HttpError -> {
                responder(kr.statusCode, "text/plain; charset=utf-8", (kr.message ?: "").encodeToByteArray())
            }
        }
    } else {
        responder(200, contentType, method.serializers.transformedResultSerializer.serializeToBytes(result))
    }

    if (exception != null) {
        handleException(exception)
    }
}