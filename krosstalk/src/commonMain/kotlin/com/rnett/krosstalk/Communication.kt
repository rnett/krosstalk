package com.rnett.krosstalk

import kotlinx.serialization.Serializable

interface ClientHandler<C : ClientScope<*>> {
    val serverUrl: String
    suspend fun sendKrosstalkRequest(
        endpoint: String,
        method: String,
        body: ByteArray,
        scopes: List<ActiveScope<*, C>>
    ): ByteArray
}

interface ServerHandler<S : ServerScope>

//TODO just use serialized parameter map, use Krosstalk serializer for this so you could use it to target existing JSON endpoint
@Serializable
data class KrosstalkCall(
    val function: String,
    val parameters: Map<String, ByteArray>
)

fun krosstalkCall(): Nothing = error("Should have been replaced with a krosstalk call during compilation")

private val valueRegex = Regex("\\{([^}]+?)\\}")

/**
 * Substitute values into an endpoint template
 */
fun fillInEndpoint(
    endpointTemplate: String,
    methodName: String,
    prefix: String,
    params: Map<String, *> = emptyMap<String, String>(),
    allowMissingParams: Boolean = false
) =
    endpointTemplate.replace(valueRegex) {
        when (val param = it.groupValues[1]) {
            methodNameKey -> methodName
            prefixKey -> prefix
            in params -> params[param].toString()
            else -> if (allowMissingParams)
                it.value
            else
                error("Endpoint template used parameter $param, but it wasn't present in arguments: ${params.keys}")
        }
    }

/**
 * Substitute values into an endpoint template, but require all parameter templates to be substituted
 *
 * @see [fillInEndpoint]
 */
fun fillInEndpointWithParams(endpointTemplate: String, methodName: String, prefix: String, params: Map<String, *>) =
    fillInEndpoint(endpointTemplate, methodName, prefix, params, allowMissingParams = false)

/**
 * Substitute method name and prefix values into an endpoint template
 *
 * @see [fillInEndpoint]
 */
fun fillInEndpointWithStatic(endpointTemplate: String, methodName: String, prefix: String) =
    fillInEndpoint(endpointTemplate, methodName, prefix, allowMissingParams = true)

@Suppress("unused")
@PublishedApi
internal suspend inline fun <T, K, reified C : ClientScope<*>> K.call(methodName: String, arguments: Map<String, *>): T
        where K : KrosstalkClient<C>, K : Krosstalk {
    val method = methods[methodName] ?: error("Unknown method $methodName")
    val serializedArgs = serialization.serializeByteArguments(arguments, method.serializers)

    val usedScopes = mutableListOf<ActiveScope<*, *>>()

    val missingRequires = mutableSetOf<String>()

    method.requiredScopes.forEach {
        val holder = _scopes.getValue(it)

        if (holder !in activeScopes)
            missingRequires += it
        else
            usedScopes += activeScopes.getValue(holder)
    }

    if (missingRequires.isNotEmpty())
        error(
            "Missing required scopes for $methodName.  Required: ${method.requiredScopes}, active: ${
                activeScopes.map { it.key.name }.toSet()
            }, missing: $missingRequires"
        )

    method.optionalScopes.forEach {
        activeScopes[_scopes.getValue(it)]?.let(usedScopes::add)
    }

    val result = client.sendKrosstalkRequest(
            fillInEndpoint(method.endpoint, methodName, this.endpointPrefix, arguments),
            method.httpMethod,
            serializedArgs,
            usedScopes.map {
                if (it.scope !is C) error("Scope ${it.scope} was not of required type ${C::class}.  This should be impossible.")
                it as ActiveScope<*, C>
            })
    return method.serializers.resultSerializer.deserializeFromBytes(result) as T
}

suspend fun <K> K.handle(method: String, data: ByteArray): ByteArray where K : Krosstalk, K : KrosstalkServer<*> {
    val method = methods[method] ?: error("No method found for $method")
    val arguments = serialization.deserializeByteArguments(data, method.serializers)

    val result = method.call(arguments)
    val resultSerializer = method.serializers.resultSerializer as Serializer<Any?, *>
    return resultSerializer.serializeToBytes(result)
}