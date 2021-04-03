package com.rnett.krosstalk.serialization

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KrosstalkException
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.extensionReceiver
import com.rnett.krosstalk.instanceReceiver
import com.rnett.krosstalk.urlDecode
import com.rnett.krosstalk.urlEncode
import kotlin.reflect.KType


/**
 * A serializer was missing for an argument.
 */
@OptIn(InternalKrosstalkApi::class)
class MissingSerializerException @InternalKrosstalkApi constructor(val argument: String, val known: Set<String>, val url: Boolean) :
    KrosstalkException(
        "Missing ${if (url) "url" else "body"} serializer for argument $argument.  Known: $known."
    )

/**
 * All necessary types for a method.
 * Instance and extension receiver serializers are in the [paramTypes] map with keys [instanceReceiver] and [extensionReceiver], respectively, but can be accessed through their accessors.
 *
 * [KrosstalkResult] return types and [KrosstalkOptional] parameters will be stored as their type params.
 */
@InternalKrosstalkApi
data class MethodTypes(
    val paramTypes: Map<String, KType>,
    val resultType: KType,
) {
    val instanceReceiverType by lazy { paramTypes[instanceReceiver] }
    val extensionReceiverSerializer by lazy { paramTypes[extensionReceiver] }
}


@InternalKrosstalkApi
data class MethodSerialization(
    private val bodySerializers: MethodArgumentSerializers<*>,
    private val urlArgSerializers: Map<String, MethodSerializer<*, Any?>>,
    private val returnValueSerializer: MethodSerializer<*, Any?>,
) {
    fun serializeReturnValue(value: Any?): ByteArray = returnValueSerializer.serializeToBytes(value)
    fun deserializeReturnValue(value: ByteArray): Any? = returnValueSerializer.deserializeFromBytes(value)

    fun serializeUrlArg(arg: String, value: Any?): String =
        urlArgSerializers.getOrElse(arg) { throw MissingSerializerException(arg, urlArgSerializers.keys, true) }.serializeToString(value)
            .urlEncode()

    fun deserializeUrlArg(arg: String, value: String): Any? =
        urlArgSerializers.getOrElse(arg) { throw MissingSerializerException(arg, urlArgSerializers.keys, true) }
            .deserializeFromString(value.urlDecode())

    fun serializeBodyArguments(arguments: Map<String, *>): ByteArray = bodySerializers.serializeArgumentsToBytes(arguments)
    fun deserializeBodyArguments(arguments: ByteArray): Map<String, *> = bodySerializers.deserializeArgumentsFromBytes(arguments)
}

/**
 * A `{Argument -> Serializer}` map, with helper functions to get the needed serializer as `Serializer<Any?, S>` rather than `Serializer<*, S>` and to serialize/deserialize all arguments.
 */
@KrosstalkPluginApi
class ArgumentSerializers<S>(val map: Map<String, Serializer<*, S>>) {
    operator fun contains(argument: String) = argument in map

    /**
     * Get a serializer for an argument as a `Serializer<Any?, S>`, throwing [MissingSerializerException] if a serializer is missing.
     */
    @OptIn(InternalKrosstalkApi::class)
    operator fun get(argument: String) = (map[argument]
        ?: throw MissingSerializerException(argument, map.keys, false)) as Serializer<Any?, S>

    /**
     * Serialize all arguments, throwing [MissingSerializerException] if a serializer is missing.
     */
    fun serializeAll(arguments: Map<String, *>): Map<String, S> = arguments.mapValues { serializeArgument(it.key, it.value) }

    /**
     * Deserialize all arguments, throwing [MissingSerializerException] if a serializer is missing.
     */
    fun deserializeAll(arguments: Map<String, S>): Map<String, Any?> = arguments.mapValues { deserializeArgument<Any?>(it.key, it.value) }

    /**
     * Serialize an argument, throwing [MissingSerializerException] if the serializer is missing.
     */
    fun <T> serializeArgument(key: String, value: T): S = this[key].serialize(value)

    /**
     * Deserialize an argument, throwing [MissingSerializerException] if the serializer is missing.
     */
    fun <T> deserializeArgument(key: String, value: S): T = this[key].deserialize(value) as T
}

@InternalKrosstalkApi
@OptIn(KrosstalkPluginApi::class)
data class MethodSerializer<S, T>(val transformer: SerializedFormatTransformer<S>, val serializer: Serializer<T, S>){
    fun serializeToBytes(data: T): ByteArray = transformer.toByteArray(serializer.serialize(data))
    fun serializeToString(data: T): String = transformer.toString(serializer.serialize(data))

    fun deserializeFromBytes(data: ByteArray): T = serializer.deserialize(transformer.fromByteArray(data))
    fun deserializeFromString(data: String): T = serializer.deserialize(transformer.fromString(data))
}

@InternalKrosstalkApi
@OptIn(KrosstalkPluginApi::class)
data class MethodArgumentSerializers<S>(val serialization: SerializationHandler<S>, val argumentSerializers: ArgumentSerializers<S>){
    fun serializeArgumentsToBytes(arguments: Map<String, *>): ByteArray = serialization.transformer.toByteArray(serialization.serializeArguments(arguments, argumentSerializers))
    fun serializeArgumentsToString(arguments: Map<String, *>): String = serialization.transformer.toString(serialization.serializeArguments(arguments, argumentSerializers))

    fun deserializeArgumentsFromBytes(arguments: ByteArray): Map<String, *> = serialization.deserializeArguments(serialization.transformer.fromByteArray(arguments), argumentSerializers)
    fun deserializeArgumentsFromString(arguments: String): Map<String, *> = serialization.deserializeArguments(serialization.transformer.fromString(arguments), argumentSerializers)

    fun <T> serializeArgumentToBytes(key: String, value: T): ByteArray = serialization.transformer.toByteArray(serialization.serializeArgument(key, value, argumentSerializers))
    fun <T> serializeArgumentToString(key: String, value: T): String = serialization.transformer.toString(serialization.serializeArgument(key, value, argumentSerializers))

    fun <T> deserializeArgumentFromBytes(key: String, value: ByteArray): T = serialization.deserializeArgument(key, serialization.transformer.fromByteArray(value), argumentSerializers)
    fun <T> deserializeArgumentFromString(key: String, value: String): T = serialization.deserializeArgument(key, serialization.transformer.fromString(value), argumentSerializers)
}