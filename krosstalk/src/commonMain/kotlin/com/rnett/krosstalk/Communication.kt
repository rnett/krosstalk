package com.rnett.krosstalk

import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor

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

sealed class Optional {
    object None : Optional()
    data class Some(val value: Any?) : Optional()

    val isSome inline get() = this is Some

    companion object {
        operator fun invoke(value: Any?) = Some(value)
    }
}

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
suspend inline fun <T, K, reified C : ClientScope<*>> K.call(methodName: String, parameters: Map<String, *>): T
        where K : KrosstalkClient<C>, K : Krosstalk {
    val method = methods[methodName] ?: error("Unknown method $methodName")
    val serializedParams = parameters.mapValues {
        val serializer = method.serializers.paramSerializers[it.key]
            ?: error("No serializer found for param ${it.key}")
        (serializer as Serializer<Any?>).serialize(it.value)
    }

    val data = Cbor.encodeToByteArray(
        KrosstalkCall.serializer(),
        KrosstalkCall(methodName, serializedParams)
    )

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
        fillInEndpoint(method.endpoint, methodName, this.endpointPrefix, parameters),
        method.httpMethod,
        data,
        usedScopes.map {
            if (it.scope !is C) error("Scope ${it.scope} was not of required type ${C::class}.  This should be impossible.")
            it as ActiveScope<*, C>
        })
    return method.serializers.resultSerializer.deserialize(result) as T
}

suspend fun <K> K.handle(data: ByteArray): ByteArray where K : Krosstalk, K : KrosstalkServer<*> {
    val call = Cbor.decodeFromByteArray(KrosstalkCall.serializer(), data)
    val method = methods[call.function] ?: error("No method found for ${call.function}")

    val params = call.parameters.mapValues {
        val serializer = method.serializers.paramSerializers[it.key]
            ?: error("No serializer found for ${it.key}")
        serializer.deserialize(it.value)
    }.toMutableMap()

    val result = method.call(params)
    val resultSerializer = method.serializers.resultSerializer as Serializer<Any?>
    return resultSerializer.serialize(result)
}