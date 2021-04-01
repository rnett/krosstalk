package com.rnett.krosstalk

import com.rnett.krosstalk.endpoint.Endpoint
import com.rnett.krosstalk.serialization.MethodSerializers
import com.rnett.krosstalk.serialization.MethodTypes
import com.rnett.krosstalk.serialization.SerializationHandler
import com.rnett.krosstalk.serialization.getAndCheckSerializers
import com.rnett.krosstalk.server.ImmutableWantedScopes


//TODO track empty body, throw if not empty?
/**
 * All the Krosstalk metedata associated with a krosstalk method.
 */
data class MethodDefinition<T>(
//        val method: KCallable<T>,
    val name: String,
    val endpoint: Endpoint,
    val httpMethod: String,
    val contentType: String?,
    val requiredScopes: Set<Scope>,
    val optionalScopes: Set<Scope>,
    val useExplicitResult: Boolean,
    val includeStacktrace: Boolean,
    val propagateServerExceptions: Boolean,
    val optionalParameters: Set<String>,
    val serializers: MethodSerializers<*>,
    val call: MethodCaller<T>,
) {

    val allScopes = requiredScopes + optionalScopes

    val hasInstanceParameter = serializers.instanceReceiverSerializer != null
    val hasExtensionParameter = serializers.extensionReceiverSerializer != null
}

// Unit and Nothing? scopes will be handled in the method
@OptIn(KrosstalkPluginApi::class)
typealias MethodCaller<T> = suspend (arguments: Map<String, *>, scopes: ImmutableWantedScopes) -> T

//TODO import from other
//TODO maybe add serializer type?
/**
 * The Krosstalk coordinator.  Krosstalk objects extend this.  Contains a listing of defined methods, the serialization handler, and optionally the client or server.
 */
abstract class Krosstalk {
    abstract val serialization: SerializationHandler<*>
    open val prefix: String = "krosstalk"

    @PublishedApi
    internal val _methods = mutableMapOf<String, MethodDefinition<*>>()

    /**
     * Methods known to this Krosstalk instance.
     */
    val methods: Map<String, MethodDefinition<*>> = _methods

    init {
        //TODO detect compiler plugin by replacement, error if not
    }

    @InternalKrosstalkApi
    fun requiredMethod(name: String) = methods[name]
        ?: throw MissingMethodException(this, name)

    private val _scopes = mutableListOf<Scope>()

    val scopes get() = _scopes.toList()

    @PublishedApi
    internal fun addScope(scope: Scope) {
        _scopes.add(scope)
    }

    @OptIn(InternalKrosstalkApi::class)
    @PublishedApi
    internal fun <T> addMethod(
        methodName: String,
        endpoint: String,
        method: String,
        contentType: String,
        types: MethodTypes,
        requiredScopes: Set<Scope>,
        optionalScopes: Set<Scope>,
        useExplicitResult: Boolean,
        includeStacktrace: Boolean,
        propagateServerExceptions: Boolean,
        optionalParameters: Set<String>,
        call: MethodCaller<T>,
    ) {
        //TODO check endpoint exclusivity

        if (methodName in methods)
            throw KrosstalkException.CompilerError("Already registered method with name $methodName, " +
                    "all but one must use `noParamHash = false` in their @KrosstalkMethod annotations.  " +
                    "If you've done this already you may have had a hash collision.")

        optionalScopes.forEach {
            if (!it.canBeOptional)
                error("Scope $it was specified as optional, but is not allowed to be.  Scope.canBeOptional was overridden at some point and set to false.")
        }

        val serializers = serialization.getAndCheckSerializers(types)
        _methods[methodName] = MethodDefinition(
            methodName,
            Endpoint(endpoint, methodName, prefix),
            method,
            contentType.ifBlank { null },
            requiredScopes,
            optionalScopes,
            useExplicitResult,
            includeStacktrace,
            propagateServerExceptions,
            optionalParameters,
            serializers,
            call
        )
    }
}

