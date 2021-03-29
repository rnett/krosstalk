package com.rnett.krosstalk.server

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.httpDecode
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
interface ServerHandler<S : ServerScope<*>>

data class KrosstalkResponse(val data: ByteArray, val exception: Throwable?) {
    fun throwException(): Nothing? {
        if (exception != null)
            throw exception
        return null
    }
}

@OptIn(InternalKrosstalkApi::class)
@KrosstalkPluginApi
suspend inline fun <K> K.handle(
    methodName: String,
    urlArguments: Map<String, String>,
    data: ByteArray,
    scopes: WantedScopes,
    handleException: (Throwable) -> Unit = { throw it },
    respond: (ByteArray) -> Unit,
): Unit where K : Krosstalk, K : KrosstalkServer<*> {
    contract {
        callsInPlace(respond, InvocationKind.EXACTLY_ONCE)
        callsInPlace(handleException, InvocationKind.AT_MOST_ONCE)
    }

    val response = handle(methodName, urlArguments, data, scopes)
    respond(response.data)
    if (response.exception != null) {
        response.exception.printStackTrace()
        handleException(response.exception)
    }
}

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
): KrosstalkResponse where K : Krosstalk, K : KrosstalkServer<*> {
    val wantedScopes = scopes.toImmutable()
    val method = requiredMethod(methodName)

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

    return KrosstalkResponse(method.serializers.transformedResultSerializer.serializeToBytes(result), exception)
}