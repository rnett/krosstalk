package com.rnett.krosstalk

import kotlin.jvm.JvmName

const val instanceParameterKey = "\$instance"
const val extensionParameterKey = "\$extension"
const val methodNameKey = "\$name"
const val prefixKey = "\$prefix"

data class MethodDefinition<T>(
//        val method: KCallable<T>,
        val endpoint: String,
        val httpMethod: String,
        val requiredScopes: Set<String>,
        val optionalScopes: Set<String>,
        val serializers: MethodSerializers<*>,
        val call: suspend (Map<String, *>) -> T
) {
    val hasInstanceParameter = serializers.instanceReceiverSerializer != null
    val hasExtensionParameter = serializers.extensionReceiverSerializer != null
}

//TODO import from other
//TODO maybe add serializer type?
abstract class Krosstalk {
    abstract val serialization: SerializationHandler<*>
    open val endpointPrefix: String = "krosstalk"

    @PublishedApi
    internal val _methods = mutableMapOf<String, MethodDefinition<*>>()

    val methods: Map<String, MethodDefinition<*>> = _methods

    @PublishedApi
    internal fun <T> addMethod(
        methodName: String,
        endpoint: String,
        method: String,
        types: MethodTypes,
        requiredScopes: Set<String>,
        optionalScopes: Set<String>,
        call: suspend (Map<String, *>) -> T
    ) {
        if (methodName in methods)
            error("Already registered method with name $methodName")

        val serializers = serialization.getAndCheckSerializers(types)
        _methods[methodName] = MethodDefinition(endpoint, method, requiredScopes, optionalScopes, serializers, call)
    }

    @PublishedApi
    internal val activeScopes = mutableMapOf<ScopeHolder, ActiveScope<*, *>>()

    fun closeAllScopes() {
        activeScopes.clear()
    }

    @PublishedApi
    internal val _scopes = mutableMapOf<String, ScopeHolder>()
}


inline val <K, C : ClientScope<*>> K.scopes: Map<String, ClientScopeHolder<C, *>> where K : Krosstalk, K : KrosstalkClient<C>
    @JvmName("clientScopes")
    get() = _scopes.mapValues { it as ClientScopeHolder<C, *> }

inline val <K, S : ServerScope> K.scopes: Map<String, ServerScopeHolder<S>> where K : Krosstalk, K : KrosstalkServer<S>
    @JvmName("serverScopes")
    get() = _scopes.mapValues { it as ServerScopeHolder<S> }

interface KrosstalkClient<C : ClientScope<*>> {
    val client: ClientHandler<C>
}


interface KrosstalkServer<S : ServerScope> {
    val server: ServerHandler<S>
}