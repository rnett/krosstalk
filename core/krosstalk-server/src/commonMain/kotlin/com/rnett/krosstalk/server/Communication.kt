package com.rnett.krosstalk.server

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.httpDecode


/**
 * A Krosstalk server handler.
 * Should help you set up receiver endpoints for all Krosstalk methods.
 * No behavior is defined here as what is necessary to do that, and where and how you will want to call it, will vary widely depending on which server you are using.
 *
 * Will almost certainly want to use [fillWithStaticAndAdjustParameters], [fillWithStatic], or [splitEndpoint] to handle endpoint templates.
 */
@KrosstalkPluginApi
interface ServerHandler<S : ServerScope<*>>


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
): ByteArray where K : Krosstalk, K : KrosstalkServer<*> {
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

    val result = if (method.useExplicitResult) {
        try {
            method.call(arguments, wantedScopes)
        } catch (e: Throwable) {
            KrosstalkResult.ServerException(e, method.includeStacktrace)
        }
    } else {
        method.call(arguments, wantedScopes)
    }

    return method.serializers.transformedResultSerializer.serializeToBytes(result)
}