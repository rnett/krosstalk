package com.rnett.krosstalk

import com.rnett.krosstalk.annotations.ExceptionHandling
import com.rnett.krosstalk.endpoint.Endpoint
import com.rnett.krosstalk.result.KrosstalkResult
import com.rnett.krosstalk.serialization.MethodSerialization
import com.rnett.krosstalk.serialization.MethodTypes
import com.rnett.krosstalk.serialization.plugin.SerializationHandler
import com.rnett.krosstalk.serialization.plugin.getArgumentSerializers
import com.rnett.krosstalk.serialization.plugin.getMethodSerializer
import com.rnett.krosstalk.server.plugin.ImmutableWantedScopes
import kotlinx.serialization.json.Json


//TODO track empty body, throw if not empty?
/**
 * All the Krosstalk metadata associated with a krosstalk method.
 *
 * @property name the method name
 * @property endpoint the method endpoint
 * @property httpMethod the http method to use
 * @property contentType the content type to use for bodies, or null to use the serialization handler's
 * @property requiredScopes scopes required by the method
 * @property optionalParameters scopes optionally used by the method
 * @property useExplicitResult whether the result is wrapped in a [KrosstalkResult]
 * @property includeStacktrace whether any [KrosstalkResult.ServerException]s should include the exception stack trace
 * @property propagateServerExceptions whether [ExceptionHandling.propagateServerExceptions] is set
 * @property optionalParameters parameters marked as optional
 * @property serverDefaultParameters parameters using [ServerDefault]
 * @property objectParameters parameters that are objects and shouldn't be transfered
 * @property returnObject the object to use as the return value, if there is one
 * @property outerWithHeaders whether to wrap the final result (including KrosstalkResult handling) in [WithHeaders]
 * @property innerWithHeaders whether to wrap the initial result (not including KrosstalkResult handling) in [WithHeaders]
 * @property requestHeadersParam the name of the parameter that contains the request headers, if there is one
 * @property serverUrlParam the name of the parameter that contains the server url, if there is one
 * @property types the method's parameter and return types
 * @property call a lambda to call the method
 * @property allScopes all scopes used by the method
 */
@OptIn(KrosstalkPluginApi::class)
@KrosstalkPluginApi
public data class MethodDefinition<T> @InternalKrosstalkApi constructor(
//        val method: KCallable<T>,
    val name: String,
    val endpoint: Endpoint,
    val httpMethod: String,
    val contentType: String?,
    val requiredScopes: Set<Scope>,
    val optionalScopes: Set<Scope>,
    @property:InternalKrosstalkApi val useExplicitResult: Boolean,
    @property:InternalKrosstalkApi val includeStacktrace: Boolean,
    @property:InternalKrosstalkApi val propagateServerExceptions: Boolean,
    @property:InternalKrosstalkApi val optionalParameters: Set<String>,
    @property:InternalKrosstalkApi val serverDefaultParameters: Set<String>,
    @property:InternalKrosstalkApi val objectParameters: Map<String, *>,
    @property:InternalKrosstalkApi val returnObject: Any?,
    @property:InternalKrosstalkApi val outerWithHeaders: Boolean,
    @property:InternalKrosstalkApi val innerWithHeaders: Boolean,
    @property:InternalKrosstalkApi val requestHeadersParam: String?,
    @property:InternalKrosstalkApi val serverUrlParam: String?,
    @property:InternalKrosstalkApi val types: MethodTypes,
    @property:InternalKrosstalkApi val serialization: MethodSerialization,
    @property:InternalKrosstalkApi val call: MethodCaller<T>,
) {

    val allScopes: Set<Scope> = requiredScopes + optionalScopes
}

// Unit and Nothing? scopes will be handled in the method
@KrosstalkPluginApi
public typealias MethodCaller<T> = suspend (arguments: Map<String, *>, scopes: ImmutableWantedScopes) -> T

@OptIn(InternalKrosstalkApi::class)
public class MissingCompilerPluginException internal constructor() :
    KrosstalkException.CompilerError("The Krosstalk compiler plugin was not used when this module was compiled!")


/**
 * A method was not registered with it's Krosstalk object.
 */
@OptIn(InternalKrosstalkApi::class, KrosstalkPluginApi::class)
public class MissingMethodException @PublishedApi internal constructor(public val krosstalkObject: Krosstalk, public val methodName: String) :
    KrosstalkException.CompilerError(
        "Krosstalk $krosstalkObject does not have a registered method named $methodName.  Known methods: ${krosstalkObject.methods}."
    )

private val serverExceptionSerializer = KrosstalkResult.ServerException.serializer()
private val serverExceptionJson = Json { }

@InternalKrosstalkApi
public fun serializeServerException(exception: KrosstalkResult.ServerException): ByteArray =
    serverExceptionJson.encodeToString(serverExceptionSerializer, exception).encodeToByteArray()

@InternalKrosstalkApi
public fun deserializeServerException(exception: ByteArray): KrosstalkResult.ServerException =
    serverExceptionJson.decodeFromString(serverExceptionSerializer, exception.decodeToString())

//TODO import from other
//TODO maybe add serializer type?

//TODO test using a abstract class between Krosstalk and object to define some things
/**
 * The Krosstalk coordinator.  Krosstalk objects extend this.  Contains a listing of defined methods, the serialization handler, and optionally the client or server.
 */
@OptIn(KrosstalkPluginApi::class)
public abstract class Krosstalk {
    public abstract val serialization: SerializationHandler<*>
    public open val urlSerialization: SerializationHandler<*> by lazy { serialization }
    public open val prefix: String = "krosstalk"

    @PublishedApi
    internal val _methods: MutableMap<String, MethodDefinition<*>> = mutableMapOf<String, MethodDefinition<*>>()

    /**
     * Methods known to this Krosstalk instance.
     */
    @KrosstalkPluginApi
    public val methods: Map<String, MethodDefinition<*>>
        get() = _methods

    @InternalKrosstalkApi
    public fun requiredMethod(name: String): MethodDefinition<*> = methods[name]
        ?: throw MissingMethodException(this, name)

    private val _scopes = mutableListOf<Scope>()

    public val scopes: List<Scope> get() = _scopes.toList()

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
        rawOptionalParameters: Set<String>,
        krosstalkOptionalParameters: Set<String>,
        objectParameters: Map<String, *>,
        returnObject: Any?,
        outerWithHeaders: Boolean,
        innerWithHeaders: Boolean,
        requestHeadersParam: String?,
        serverUrlParam: String?,
        call: MethodCaller<T>,
    ) {
        //TODO check endpoint exclusivity

        if (methodName in methods)
            throw KrosstalkException.CompilerError(
                "Already registered method with name $methodName, " +
                        "all but one must use `noParamHash = false` in their @KrosstalkMethod annotations.  " +
                        "If you've done this already you may have had a hash collision."
            )

        optionalScopes.forEach {
            if (!it.canBeOptional)
                error("Scope $it was specified as optional, but is not allowed to be.  Scope.canBeOptional was overridden at some point and set to false.")
        }


        val builtEndpoint = Endpoint(endpoint, methodName, prefix)
        _methods[methodName] = MethodDefinition(
            methodName,
            builtEndpoint,
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
            requestHeadersParam,
            serverUrlParam,
            types,
            MethodSerialization(
                serialization.getArgumentSerializers(types),
                builtEndpoint.allReferencedParameters().associateWith { urlSerialization.getMethodSerializer<Any?>(types.paramTypes.getValue(it)) },
                types.resultType?.let(serialization::getMethodSerializer)
            ),
            call
        )
    }
}

