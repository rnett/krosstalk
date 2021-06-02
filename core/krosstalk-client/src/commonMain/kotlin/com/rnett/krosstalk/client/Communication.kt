package com.rnett.krosstalk.client

import com.rnett.krosstalk.Headers
import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KROSSTALK_THROW_EXCEPTION_HEADER_NAME
import com.rnett.krosstalk.KROSSTALK_UNCAUGHT_EXCEPTION_HEADER_NAME
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkException
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.MethodDefinition
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.ServerDefault
import com.rnett.krosstalk.WithHeaders
import com.rnett.krosstalk.annotations.ServerURL
import com.rnett.krosstalk.client.plugin.AppliedClientScope
import com.rnett.krosstalk.client.plugin.toAppliedScope
import com.rnett.krosstalk.client.plugin.ClientScope
import com.rnett.krosstalk.deserializeServerException
import com.rnett.krosstalk.headersOf
import com.rnett.krosstalk.isNone
import com.rnett.krosstalk.result.KrosstalkResult
import com.rnett.krosstalk.result.KrosstalkUncaughtServerException
import com.rnett.krosstalk.result.isFailure
import com.rnett.krosstalk.result.isServerException
import com.rnett.krosstalk.result.valueOrThrow
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Placeholder for a Krosstalk client side method.  Will be replaced by the compiler plugin.
 */
@OptIn(InternalKrosstalkApi::class)
public suspend fun krosstalkCall(): Nothing =
    throw KrosstalkException.CompilerError("Should have been replaced with a krosstalk call during compilation")

/**
 * Placeholder for a Krosstalk client side method.  Will be replaced by the compiler plugin.
 *
 * [serverUrl] will override the [KrosstalkClient.serverUrl] if non-null, and be overridden by [ServerURL] parameters in turn.
 */
@OptIn(InternalKrosstalkApi::class)
public suspend fun krosstalkCall(serverUrl: String?): Nothing =
    throw KrosstalkException.CompilerError("Should have been replaced with a krosstalk call during compilation")

/**
 * Placeholder for a Krosstalk client side method.  Will be replaced by the compiler plugin.
 *
 * [requestHeaders] will be added to the request (along with request headers added anywhere else).
 */
@OptIn(InternalKrosstalkApi::class)
public suspend fun krosstalkCall(requestHeaders: Headers): Nothing =
    throw KrosstalkException.CompilerError("Should have been replaced with a krosstalk call during compilation")

/**
 * Placeholder for a Krosstalk client side method.  Will be replaced by the compiler plugin.
 *
 * [scopes] are additional scopes to apply to the call.
 * Passing an instance of a scope already specified by a parameter **will cause a [DuplicateScopeException] when the method is called.**
 */
@OptIn(InternalKrosstalkApi::class)
public suspend fun krosstalkCall(vararg scopes: ScopeInstance<*>): Nothing =
    throw KrosstalkException.CompilerError("Should have been replaced with a krosstalk call during compilation")

/**
 * Placeholder for a Krosstalk client side method.  Will be replaced by the compiler plugin.
 *
 * [scopes] are additional scopes to apply to the call.
 * Passing an instance of a scope already specified by a parameter **will cause a [DuplicateScopeException] when the method is called.**
 */
@OptIn(InternalKrosstalkApi::class)
public suspend fun krosstalkCall(scopes: Iterable<ScopeInstance<*>>): Nothing =
    throw KrosstalkException.CompilerError("Should have been replaced with a krosstalk call during compilation")

/**
 * Placeholder for a Krosstalk client side method.  Will be replaced by the compiler plugin.
 *
 * [serverUrl] will override the [KrosstalkClient.serverUrl] if non-null, and be overridden by [ServerURL] parameters in turn.
 *
 * [requestHeaders] will be added to the request (along with request headers added anywhere else).
 *
 * [scopes] are additional scopes to apply to the call.
 * Passing an instance of a scope already specified by a parameter **will cause a [DuplicateScopeException] when the method is called.**
 */
@OptIn(InternalKrosstalkApi::class)
public suspend fun krosstalkCall(
    serverUrl: String? = null,
    requestHeaders: Headers = headersOf(),
    scopes: Iterable<ScopeInstance<*>> = emptyList()
): Nothing =
    throw KrosstalkException.CompilerError("Should have been replaced with a krosstalk call during compilation")

/**
 * Placeholder for a Krosstalk client side method.  Will be replaced by the compiler plugin.
 *
 * [serverUrl] will override the [KrosstalkClient.serverUrl] if non-null, and be overridden by [ServerURL] parameters in turn.
 *
 * [requestHeaders] will be added to the request (along with request headers added anywhere else).
 *
 * [scopes] are additional scopes to apply to the call.
 * Passing an instance of a scope already specified by a parameter **will cause a [DuplicateScopeException] when the method is called.**
 */
@OptIn(InternalKrosstalkApi::class)
public suspend fun krosstalkCall(serverUrl: String? = null, requestHeaders: Headers = headersOf(), vararg scopes: ScopeInstance<*>): Nothing =
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
) : KrosstalkException("Method $methodName failed with a client exception: $cause", cause)

@OptIn(InternalKrosstalkApi::class)
public class WrongHeadersTypeException @InternalKrosstalkApi constructor(
    public val methodName: String,
    public val type: KClass<*>
) :
    KrosstalkException(
        "Invalid type for request headers param: Map<String, List<String>> is required, got $type."
    )

@OptIn(InternalKrosstalkApi::class)
public class WrongScopeTypeException @InternalKrosstalkApi constructor(
    public val scope: Scope,
    public val required: KType
) : KrosstalkException("krosstalkCall scope $scope is not of client type $required")

@OptIn(InternalKrosstalkApi::class)
public class DuplicateScopeException @InternalKrosstalkApi constructor(
    public val scope: Scope
) : KrosstalkException("krosstalkCall scope $scope was already specified by a parameter")

@KrosstalkPluginApi
@InternalKrosstalkApi
@PublishedApi
internal fun <T> MethodDefinition<T>.getReturnValue(data: ByteArray): T = if (returnObject != null) {
    @Suppress("UNCHECKED_CAST")
    returnObject as T
} else {
    @Suppress("UNCHECKED_CAST")
    serialization.deserializeReturnValue(data) as T
}

@PublishedApi
internal fun Any?.withHeadersIf(withHeaders: Boolean, headers: Headers): Any? =
    if (withHeaders) WithHeaders(this, headers) else this

@OptIn(KrosstalkPluginApi::class, ExperimentalStdlibApi::class, InternalKrosstalkApi::class)
@PublishedApi
internal inline fun <reified C : ClientScope<*>> KrosstalkClient<C>.toAppliedScopeWithType(scope: ScopeInstance<*>): AppliedClientScope<C, *> {
    if (scope.scope !is C)
        throw WrongScopeTypeException(scope.scope, typeOf<C>())
    @Suppress("UNCHECKED_CAST")
    return (scope as ScopeInstance<C>).toAppliedScope()!!
}

@OptIn(InternalKrosstalkApi::class, KrosstalkPluginApi::class, ExperimentalStdlibApi::class)
@Suppress("unused")
@PublishedApi
internal suspend inline fun <T, K, reified C : ClientScope<*>> K.call(
    methodName: String,
    rawArguments: Map<String, *>,
    methodScopes: List<AppliedClientScope<C, *>>,
    callServerUrl: String?,
    callRequestHeaders: Headers?,
    callScopes: Iterable<ScopeInstance<*>>?
): T where K : KrosstalkClient<C>, K : Krosstalk {

    val method = requiredMethod(methodName)

    val requestHeaders = (callRequestHeaders ?: headersOf()) +
            if (method.requestHeadersParam != null) {
                rawArguments.getValue(method.requestHeadersParam!!)!!
                    .let { it as? Headers ?: throw WrongHeadersTypeException(methodName, it::class) }
            } else
                headersOf()

    val serverUrl = method.serverUrlParam?.let { rawArguments.getValue(it) as String? } ?: callServerUrl ?: this.serverUrl

    val paramScopes = methodScopes.map { it.scope }.toSet()
    callScopes?.map { it.scope }?.forEach {
        if (it in paramScopes)
            throw DuplicateScopeException(it)
    }

    val scopes = methodScopes + callScopes?.map { toAppliedScopeWithType(it) }.orEmpty()

    @Suppress("DEPRECATION")
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
        arguments.filter { it.value != null }.keys // note it's only optionals, so is required to be @Optional
    ) {
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
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
            requestHeaders,
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

    if (wrappedResult.isFailure() &&
        result.headers.anyOf(KROSSTALK_THROW_EXCEPTION_HEADER_NAME) { it.toBooleanStrictOrNull() == true }
    ) {
        if (wrappedResult.isServerException() &&
            result.headers.anyOf(KROSSTALK_UNCAUGHT_EXCEPTION_HEADER_NAME) { it.toBooleanStrictOrNull() == true }
        )
            throw KrosstalkUncaughtServerException(wrappedResult as KrosstalkResult.ServerException)
        else
            wrappedResult.throwFailureException()
    }

    @Suppress("UNCHECKED_CAST")
    return if (method.useExplicitResult) {
        wrappedResult
    } else {
        wrappedResult.valueOrThrow
    }.withHeadersIf(method.outerWithHeaders, result.headers) as T
}
