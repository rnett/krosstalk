package com.rnett.krosstalk

import com.rnett.krosstalk.annotations.ExplicitResult
import com.rnett.krosstalk.endpoint.Endpoint
import com.rnett.krosstalk.serialization.MethodSerialization
import com.rnett.krosstalk.serialization.MethodTypes
import com.rnett.krosstalk.serialization.SerializationHandler
import com.rnett.krosstalk.serialization.getArgumentSerializers
import com.rnett.krosstalk.serialization.getMethodSerializer
import com.rnett.krosstalk.server.ImmutableWantedScopes


//TODO track empty body, throw if not empty?
/**
 * All the Krosstalk metedata associated with a krosstalk method.
 *
 * @property name the method name
 * @property endpoint the method endpoint
 * @property httpMethod the http method to use
 * @property contentType the content type to use for bodies, or null to use the serialization handler's
 * @property requiredScopes scopes required by the method
 * @property optionalParameters scopes optionally used by the method
 * @property useExplicitResult whether the result is wrapped in a [KrosstalkResult]
 * @property includeStacktrace whether any [KrosstalkResult.ServerException]s should include the exception stack trace
 * @property propagateServerExceptions whether [ExplicitResult.propagateServerExceptions] is set
 * @property optionalParameters parameters marked as optional
 * @property serverDefaultParameters parameters using [ServerDefault]
 * @property objectParameters parameters that are objects and shouldn't be transfered
 * @property returnObject the object to use as the return value, if there is one
 * @property outerWithHeaders whether to wrap the final result (including KrosstalkResult handling) in [WithHeaders]
 * @property innerWithHeaders whether to wrap the initial result (not including KrosstalkResult handling) in [WithHeaders]
 * @property types the method's parameter and return types
 * @property call a lambda to call the method
 * @property allScopes all scopes used by the method
 */
data class MethodDefinition<T> @InternalKrosstalkApi constructor(
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
    val serverDefaultParameters: Set<String>,
    val objectParameters: Map<String, *>,
    val returnObject: Any?,
    val outerWithHeaders: Boolean,
    val innerWithHeaders: Boolean,
    @InternalKrosstalkApi
    val types: MethodTypes,
    @InternalKrosstalkApi val serialization: MethodSerialization,
    val call: MethodCaller<T>,
) {

    val allScopes = requiredScopes + optionalScopes
}

// Unit and Nothing? scopes will be handled in the method
@OptIn(KrosstalkPluginApi::class)
typealias MethodCaller<T> = suspend (arguments: Map<String, *>, scopes: ImmutableWantedScopes) -> T

@OptIn(InternalKrosstalkApi::class)
class MissingCompilerPluginException internal constructor() :
    KrosstalkException.CompilerError("The Krosstalk compiler plugin was not used when this module was compiled!")


/**
 * A method was not registered with it's Krosstalk object.
 */
@OptIn(InternalKrosstalkApi::class)
class MissingMethodException @PublishedApi internal constructor(val krosstalkObject: Krosstalk, val methodName: String) :
    KrosstalkException.CompilerError(
        "Krosstalk $krosstalkObject does not have a registered method named $methodName.  Known methods: ${krosstalkObject.methods}."
    )
//TODO import from other
//TODO maybe add serializer type?
/**
 * The Krosstalk coordinator.  Krosstalk objects extend this.  Contains a listing of defined methods, the serialization handler, and optionally the client or server.
 */
@OptIn(KrosstalkPluginApi::class)
abstract class Krosstalk {
    abstract val serialization: SerializationHandler<*>
    open val urlSerialization: SerializationHandler<*> by lazy { serialization }
    open val prefix: String = "krosstalk"

    @PublishedApi
    internal val _methods = mutableMapOf<String, MethodDefinition<*>>()

    /**
     * Methods known to this Krosstalk instance.
     */
    val methods: Map<String, MethodDefinition<*>> = _methods

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
    private val serverExceptionSerializer by lazy { urlSerialization.getMethodSerializer<KrosstalkResult.ServerException>() }

    @InternalKrosstalkApi
    fun serializeServerException(exception: KrosstalkResult.ServerException) = serverExceptionSerializer.serializeToBytes(exception)

    @InternalKrosstalkApi
    fun deserializeServerException(exception: ByteArray) = serverExceptionSerializer.deserializeFromBytes(exception)

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
        rawOptionalParameters: Set<String>,
        krosstalkOptionalParameters: Set<String>,
        objectParameters: Map<String, *>,
        returnObject: Any?,
        outerWithHeaders: Boolean,
        innerWithHeaders: Boolean,
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


        val endpoint = Endpoint(endpoint, methodName, prefix)
        _methods[methodName] = MethodDefinition(
            methodName,
            endpoint,
            method,
            contentType.ifBlank { null },
            requiredScopes,
            optionalScopes,
            useExplicitResult,
            includeStacktrace,
            propagateServerExceptions,
            rawOptionalParameters,
            krosstalkOptionalParameters,
            objectParameters,
            returnObject,
            outerWithHeaders,
            innerWithHeaders,
            types,
            MethodSerialization(
                serialization.getArgumentSerializers(types),
                endpoint.allReferencedParameters().associateWith { urlSerialization.getMethodSerializer<Any?>(types.paramTypes.getValue(it)) },
                types.resultType?.let(serialization::getMethodSerializer)
            ),
            call
        )
    }
}

