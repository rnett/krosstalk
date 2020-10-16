package com.rnett.krosstalk

import com.rnett.krosstalk.annotations.KrosstalkEndpoint
import com.rnett.krosstalk.serialization.MethodSerializers
import com.rnett.krosstalk.serialization.MethodTypes
import com.rnett.krosstalk.serialization.SerializationHandler
import com.rnett.krosstalk.serialization.getAndCheckSerializers

/**
 * The key used for instance/dispatch receiver parameters/arguments in parameter/argument maps and [KrosstalkEndpoint] templates.
 */
const val instanceParameterKey = "\$instance"

/**
 * The key used for extension receiver parameters/arguments in parameter/argument maps and [KrosstalkEndpoint] templates.
 */
const val extensionParameterKey = "\$extension"

/**
 * The key to use the method's name in a [KrosstalkEndpoint] template.
 */
const val methodNameKey = "\$name"

/**
 * The key to use the Krosstalk object's [Krosstalk.endpointPrefix] in a [KrosstalkEndpoint] template.
 */
const val prefixKey = "\$prefix"

/**
 * All the Krosstalk metedata associated with a krosstalk method.
 */
data class MethodDefinition<T>(
//        val method: KCallable<T>,
    private val _endpoint: String,
    val httpMethod: String,
    val requiredScopes: Set<String>,
    val optionalScopes: Set<String>,
    val leaveOutArguments: Boolean,
    val nullOnResponseCodes: Set<Int>,
    val useExplicitResult: Boolean,
    val includeStacktrace: Boolean,
    val serializers: MethodSerializers<*>,
    val call: suspend (Map<String, *>) -> T
) {
    val endpoint = Endpoint(_endpoint)

    val hasInstanceParameter = serializers.instanceReceiverSerializer != null
    val hasExtensionParameter = serializers.extensionReceiverSerializer != null

    fun <V> bodyArguments(map: Map<String, V>) = if(leaveOutArguments) map.filterKeys { it !in endpoint.usedArguments } else map
    fun <V> urlArguments(map: Map<String, V>) = if(leaveOutArguments) map.filterKeys { it in endpoint.usedArguments } else emptyMap()

    val hasBodyArguments = bodyArguments(serializers.paramSerializers.map).isNotEmpty()
}

//TODO import from other
//TODO maybe add serializer type?
/**
 * The Krosstalk coordinator.  Krosstalk objects extend this.  Contains a listing of defined methods, the serialization handler, and optionally the client or server.
 */
abstract class Krosstalk {
    abstract val serialization: SerializationHandler<*>
    open val endpointPrefix: String = "krosstalk"

    @PublishedApi
    internal val _methods = mutableMapOf<String, MethodDefinition<*>>()

    /**
     * Methods known to this Krosstalk instance.
     */
    val methods: Map<String, MethodDefinition<*>> = _methods

    init {
        //TODO detect compiler plugin by replacement, error if not
    }

    @PublishedApi
    internal fun requiredMethod(name: String) = methods[name]
        ?: throw KrosstalkException.MissingMethod(this, name)

    @PublishedApi
    internal fun <T> addMethod(
        methodName: String,
        endpoint: String,
        method: String,
        types: MethodTypes,
        requiredScopes: Set<String>,
        optionalScopes: Set<String>,
        leaveOutArguments: Boolean,
        nullOnResponses: Set<Int>,
        useExplicitResult: Boolean,
        includeStacktrace: Boolean,
//        annotations: Map<KClass<Annotation>, Map<String, Any?>>,
        call: suspend (Map<String, *>) -> T
    ) {
        if (methodName in methods)
            throw KrosstalkException.CompilerError("Already registered method with name $methodName.")

        val serializers = serialization.getAndCheckSerializers(types)
        _methods[methodName] = MethodDefinition(
            endpoint,
            method,
            requiredScopes,
            optionalScopes,
            leaveOutArguments,
            nullOnResponses,
            useExplicitResult,
            includeStacktrace,
            serializers,
            call
        )
    }

    @PublishedApi
    internal val activeScopes = mutableMapOf<ScopeHolder, ActiveScope<*, *>>()

    /**
     * Close all active scopes.  Be careful with this.
     */
    fun closeAllScopes() {
        activeScopes.clear()
    }

    @PublishedApi
    internal val _scopes = mutableMapOf<String, ScopeHolder>()
}

/**
 * The interface for a krosstalk client.  Have your Krosstalk object implement this to be a client.
 */
interface KrosstalkClient<C : ClientScope<*>> {
    val client: ClientHandler<C>
}


/**
 * The interface for a krosstalk server.  Have your Krosstalk object implement this to be a server.
 */
interface KrosstalkServer<S : ServerScope> {
    val server: ServerHandler<S>
}