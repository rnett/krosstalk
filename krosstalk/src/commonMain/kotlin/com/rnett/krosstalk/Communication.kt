package com.rnett.krosstalk

//TODO I really need custom exceptions in here

sealed class KrosstalkResponse {
    abstract val responseCode: Int

    data class Success(override val responseCode: Int = 200, val data: ByteArray) : KrosstalkResponse() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false

            if (responseCode != other.responseCode) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = responseCode
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    data class Failure(override val responseCode: Int = 200, val error: (suspend (methodName: String) -> Nothing)? = null) : KrosstalkResponse()
}

interface ClientHandler<C : ClientScope<*>> {
    val serverUrl: String
    suspend fun sendKrosstalkRequest(
            endpoint: String,
            method: String,
            body: ByteArray?,
            scopes: List<ActiveScope<*, C>>
    ): KrosstalkResponse
}

interface ServerHandler<S : ServerScope>

suspend fun krosstalkCall(): Nothing = error("Should have been replaced with a krosstalk call during compilation")

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
    val keptArgs = arguments.filterKeys { it !in method.leaveOutArguments }
    val serializedArgs = serialization.serializeByteArguments(keptArgs, method.serializers)

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
            if (keptArgs.isEmpty()) null else serializedArgs,
            usedScopes.map {
                if (it.scope !is C) error("Scope ${it.scope} was not of required type ${C::class}.  This should be impossible.")
                it as ActiveScope<*, C>
            })

    @Suppress("UNCHECKED_CAST") // checked in compiler plugin
    if (result.responseCode in method.nullOnResponseCodes)
        return null as T

    return when (result) {
        is KrosstalkResponse.Success -> method.serializers.resultSerializer.deserializeFromBytes(result.data) as T
        is KrosstalkResponse.Failure -> result.error?.let { it(methodName) }
                ?: error("Krosstalk method $methodName failed with HTTP status code ${result.responseCode}")
    }
}

suspend fun <K> K.handle(method: String, data: ByteArray): ByteArray where K : Krosstalk, K : KrosstalkServer<*> {
    val method = methods[method] ?: error("No method found for $method")

    if (method.leaveOutArguments.isNotEmpty())
        error("You somehow set MinimizeBody or EmptyBody on a call with a krosstalk server.  It should cause a compiler error, this is a bug.")

    val arguments = serialization.deserializeByteArguments(data, method.serializers)

    val result = method.call(arguments)
    val resultSerializer = method.serializers.resultSerializer as Serializer<Any?, *>
    return resultSerializer.serializeToBytes(result)
}