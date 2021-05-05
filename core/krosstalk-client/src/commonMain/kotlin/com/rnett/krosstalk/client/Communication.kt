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
import com.rnett.krosstalk.client.plugin.AppliedClientScope
import com.rnett.krosstalk.client.plugin.ClientScope
import com.rnett.krosstalk.isNone
import kotlin.reflect.KClass

/**
 * Placeholder for a Krosstalk client side method.  Will be replaced by the compiler plugin.
 */
@OptIn(InternalKrosstalkApi::class)
public suspend fun krosstalkCall(): Nothing =
    throw KrosstalkException.CompilerError("Should have been replaced with a krosstalk call during compilation")

@InternalKrosstalkApi
public class ServerDefaultInEndpointException(public val methodName: String, public val parameter: String) :
    KrosstalkException("Parameter \"$parameter\" for method \"$methodName\" was an unrealized ServerDefault, but was used in the endpoint.")

/**
 * A Krosstalk call failed in the client.  Happens when a request could not be made.
 */
@OptIn(InternalKrosstalkApi::class)
public open class ClientFailureException @InternalKrosstalkApi constructor(
    public val methodName: String,
    override val cause: Throwable
) : KrosstalkException("Method $methodName failed with a client exception", cause)

@OptIn(InternalKrosstalkApi::class)
public class WrongHeadersTypeException @InternalKrosstalkApi constructor(
    public val methodName: String,
    public val type: KClass<*>
) :
    KrosstalkException(
        "Invalid type for request headers param: Map<String, List<String>> is required, got $type."
    )

@KrosstalkPluginApi
@InternalKrosstalkApi
@PublishedApi
internal fun <T> MethodDefinition<T>.getReturnValue(data: ByteArray): T = if (returnObject != null) {
    returnObject as T
} else {
    serialization.deserializeReturnValue(data) as T
}

@PublishedApi
internal fun Any?.withHeadersIf(withHeaders: Boolean, headers: Headers): Any? =
    if (withHeaders) WithHeaders(this, headers) else this

//TODO finish result changes (will runKrosstalkCatching pick them up?) unify server/client handling as much as possible
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
        rawArguments.getValue(method.requestHeadersParam!!)!!
            .let { it as? Headers ?: throw WrongHeadersTypeException(methodName, it::class) }
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
        .filterNot { it.value == null && it.key in method.optionalParameters }.minus(method.objectParameters.keys)
        .minus(usedInUrl)

    val serializedBody = method.serialization.serializeBodyArguments(bodyArguments)

    val result = try {
        client.sendKrosstalkRequest(
            serverUrl.trimEnd('/') + "/" + endpoint.trimStart('/'),
            method.httpMethod,
            method.contentType ?: serialization.contentType,
            requestHeaders ?: emptyMap(),
            if (bodyArguments.isEmpty()) null else serializedBody,
            scopes
        )
    } catch (e: Throwable) {
        throw ClientFailureException(methodName, e)
    }

    val wrappedResult =
        if (result.isSuccess()) {
            KrosstalkResult.Success(
                method.getReturnValue(result.body).withHeadersIf(method.innerWithHeaders, result.headers)
            )
        } else {
            if (result.statusCode == 500) {
                try {
                    deserializeServerException(result.body)
                } catch (t: Throwable) {
                    KrosstalkResult.HttpError(result.statusCode, result.stringBody)
                }
            } else {
                KrosstalkResult.HttpError(result.statusCode, result.stringBody)
            }
        }

    return if (method.useExplicitResult) {
        wrappedResult
    } else {
        wrappedResult.valueOrThrow
    }.withHeadersIf(method.outerWithHeaders, result.headers) as T
}
