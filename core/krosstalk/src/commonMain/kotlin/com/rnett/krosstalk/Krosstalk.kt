package com.rnett.krosstalk

import com.rnett.krosstalk.endpoint.Endpoint
import com.rnett.krosstalk.serialization.MethodSerializers
import com.rnett.krosstalk.serialization.MethodTypes
import com.rnett.krosstalk.serialization.SerializationHandler
import com.rnett.krosstalk.serialization.getAndCheckSerializers

//TODO change \$ to %?


/**
 * All the Krosstalk metedata associated with a krosstalk method.
 */
data class MethodDefinition<T>(
//        val method: KCallable<T>,
    val name: String,
    val endpoint: Endpoint,
    val httpMethod: String,
    val requiredScopes: Set<Scope>,
    val optionalScopes: Set<Scope>,
    val minimizeBody: Boolean,
    val nullOnResponseCodes: Set<Int>,
    val useExplicitResult: Boolean,
    val includeStacktrace: Boolean,
    val serializers: MethodSerializers<*>,
    val call: MethodCaller<T>,
) {

    val hasInstanceParameter = serializers.instanceReceiverSerializer != null
    val hasExtensionParameter = serializers.extensionReceiverSerializer != null

    fun bodyArguments(arguments: Map<String, *>) =
        if (minimizeBody) arguments.filterKeys { it !in endpoint.usedParameters(arguments.filter { it.value != null }.keys) } else arguments

    fun urlArguments(arguments: Map<String, *>) =
        if (minimizeBody) arguments.filterKeys { it in endpoint.usedParameters(arguments.filter { it.value != null }.keys) } else emptyMap()

    fun hasBodyArguments(arguments: Map<String, *>) = bodyArguments(arguments).isNotEmpty()
}

// Unit and Nothing? scopes will be handled in the method
typealias MethodCaller<T> = suspend (arguments: Map<String, *>, scopes: ImmutableWantedScopes) -> T

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
        ?: throw MissingMethodException(this, name)

    private val _scopes = mutableListOf<Scope>()

    val scopes get() = _scopes.toList()

    @PublishedApi
    internal fun addScope(scope: Scope) {
        _scopes.add(scope)
    }

    @PublishedApi
    internal fun <T> addMethod(
        methodName: String,
        endpoint: String,
        method: String,
        types: MethodTypes,
        requiredScopes: Set<Scope>,
        optionalScopes: Set<Scope>,
        leaveOutArguments: Boolean,
        nullOnResponses: Set<Int>,
        useExplicitResult: Boolean,
        includeStacktrace: Boolean,
//        annotations: Map<KClass<Annotation>, Map<String, Any?>>,
        call: MethodCaller<T>,
    ) {
        //TODO check endpoint exclusivity

        if (methodName in methods)
            throw KrosstalkException.CompilerError("Already registered method with name $methodName.")

        optionalScopes.forEach {
            if (!it.canBeOptional)
                error("Scope $it was specified as optional, but is not allowed to be.  Scope.canBeOptional was overridden at some point and set to false.")
        }

        val serializers = serialization.getAndCheckSerializers(types)
        _methods[methodName] = MethodDefinition(
            methodName,
            Endpoint(endpoint, methodName, endpointPrefix),
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
interface KrosstalkServer<S : ServerScope<*>> {
    val server: ServerHandler<S>
}