package com.rnett.krosstalk.serialization

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KrosstalkException
import com.rnett.krosstalk.extensionParameter
import com.rnett.krosstalk.instanceParameter
import kotlin.reflect.KType

//TODO MethodSerializer class of Serializer + SerializationHandler?  could have direct serializeBytes/serializeString

/**
 * All necessary serializers for a method.
 * Instance and extension receiver serializers are in the [paramSerializers] map with keys [instanceParameter] and [extensionParameter], respectively, but can be accessed through their accessors.
 */
data class MethodSerializers<S> @PublishedApi internal constructor(
    val serialization: SerializationHandler<S>,
    val paramSerializers: ArgumentSerializers<S>,
    val resultSerializer: Serializer<*, S>
) {
    val instanceReceiverSerializer by lazy { paramSerializers.map[instanceParameter] }
    val extensionReceiverSerializer by lazy { paramSerializers.map[extensionParameter] }

    val transformedResultSerializer by lazy { MethodSerializer(serialization.transformer, resultSerializer) as MethodSerializer<S, Any?> }
    val transformedParamSerializers by lazy { MethodArgumentSerializers(serialization, paramSerializers) }

}

/**
 * All necessary types for a method.
 * Instance and extension receiver serializers are in the [paramTypes] map with keys [instanceParameter] and [extensionParameter], respectively, but can be accessed through their accessors.
 */
class MethodTypes(
    val paramTypes: Map<String, KType>,
    val resultType: KType
) {
    inline fun <S> toSerializers(serialization: SerializationHandler<S>, getSerializer: (KType) -> Serializer<*, S>) = MethodSerializers(
        serialization,
        ArgumentSerializers(paramTypes.mapValues { getSerializer(it.value) }),
        getSerializer(resultType)
    )

    val instanceReceiverType by lazy { paramTypes[instanceParameter] }
    val extensionReceiverSerializer by lazy { paramTypes[extensionParameter] }

    /**
     * Ensure that a [MethodSerializers] has all nessecary serializers for this method.
     */
    fun checkSerializers(serializers: MethodSerializers<*>) {
        paramTypes.keys.forEach {
            check(it in serializers.paramSerializers) { "Missing serializer for $it" }
        }
    }
}

/**
 * A `{Argument -> Serializer}` map, with helper functions to get the needed serializer as `Serializer<Any?, S>` rather than `Serializer<*, S>` and to serialize/deserialize all arguments.
 */
class ArgumentSerializers<S>(val map: Map<String, Serializer<*, S>>) {
    operator fun contains(argument: String) = argument in map

    /**
     * Get a serializer for an argument as a `Serializer<Any?, S>`, throwing [KrosstalkException.MissingSerializer] if a serializer is missing.
     */
    @OptIn(InternalKrosstalkApi::class)
    operator fun get(argument: String) = (map[argument]
        ?: throw KrosstalkException.MissingSerializer(argument, map.keys)) as Serializer<Any?, S>

    /**
     * Serialize all arguments, throwing [KrosstalkException.MissingSerializer] if a serializer is missing.
     */
    fun serializeAll(arguments: Map<String, *>): Map<String, S> = arguments.mapValues { serializeArgument(it.key, it.value) }

    /**
     * Deserialize all arguments, throwing [KrosstalkException.MissingSerializer] if a serializer is missing.
     */
    fun deserializeAll(arguments: Map<String, S>): Map<String, Any?> = arguments.mapValues { deserializeArgument<Any?>(it.key, it.value) }

    /**
     * Serialize an argument, throwing [KrosstalkException.MissingSerializer] if the serializer is missing.
     */
    fun <T> serializeArgument(key: String, value: T): S = this[key].serialize(value)

    /**
     * Deserialize an argument, throwing [KrosstalkException.MissingSerializer] if the serializer is missing.
     */
    fun <T> deserializeArgument(key: String, value: S): T = this[key].deserialize(value) as T
}

data class MethodSerializer<S, T>(val transformer: SerializedFormatTransformer<S>, val serializer: Serializer<T, S>){
    fun serializeToBytes(data: T): ByteArray = transformer.toByteArray(serializer.serialize(data))
    fun serializeToString(data: T): String = transformer.toString(serializer.serialize(data))

    fun deserializeFromBytes(data: ByteArray): T = serializer.deserialize(transformer.fromByteArray(data))
    fun deserializeFromString(data: String): T = serializer.deserialize(transformer.fromString(data))
}

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