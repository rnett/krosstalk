package com.rnett.krosstalk.serialization

import kotlin.reflect.KType
import kotlin.reflect.typeOf


/**
 * A SerializationHandler capable of getting serializers from [KType]s,
 * serializing and deserializing argument maps to some intermediate data type [S],
 * and turning [S]s into [ByteArray]s and [String]s and visa versa.
 *
 * Requires manual serialization of the argument maps for support for things like putting them in a JSON object.
 * To have each argument automatically serialized use [StringArgumentSerializationHandler] or [BinaryArgumentSerializationHandler].
 *
 */
interface SerializationHandler<S> {
    /**
     * Get a [MethodSerializers] from a [MethodTypes].  By default uses [MethodTypes.toSerializers] with [getSerializer].
     */
    fun getSerializers(types: MethodTypes): MethodSerializers<S> = types.toSerializers(this) {
        getSerializer(it)
    }

    /**
     * Get a serializer for a [KType].
     */
    fun getSerializer(type: KType): Serializer<*, S>

    /**
     * Serialize a argument map to [S] using the provided argument serializers.
     */
    fun serializeArguments(arguments: Map<String, *>, serializers: ArgumentSerializers<S>): S

    /**
     * Deserialize an argument map from [S] using the provided argument serializers.
     */
    fun deserializeArguments(arguments: S, serializers: ArgumentSerializers<S>): Map<String, *>

    /**
     * Serialize a single argument to [S].
     */
    fun <T> serializeArgument(key: String, value: T, serializers: ArgumentSerializers<S>): S = serializers.serializeArgument(key, value)

    /**
     * Deserialize a single argument from [S].
     */
    fun <T> deserializeArgument(key: String, value: S, serializers: ArgumentSerializers<S>): T = serializers.deserializeArgument(key, value)

    val transformer: SerializedFormatTransformer<S>
}


abstract class BaseSerializationHandler<S>(override val transformer: SerializedFormatTransformer<S>): SerializationHandler<S>

/**
 * A [StringSerializationHandler] that automatically serializes/deserializes each argument before calling [serializeArguments]/[deserializeArguments].
 */
abstract class ArgumentSerializationHandler<S>(transformer: SerializedFormatTransformer<S>) : BaseSerializationHandler<S>(transformer) {
    /**
     * Combine serialized arguments into a final serialized form.
     */
    abstract fun serializeArguments(serializedArguments: Map<String, S>): S

    /**
     * Deconstruct the final serialized form into serialized arguments.
     */
    abstract fun deserializeArguments(arguments: S): Map<String, S>

    final override fun serializeArguments(arguments: Map<String, *>, serializers: ArgumentSerializers<S>): S {
        return serializeArguments(serializers.serializeAll(arguments))
    }

    final override fun deserializeArguments(arguments: S, serializers: ArgumentSerializers<S>): Map<String, *> {
        val serializedArguments = deserializeArguments(arguments)
        return serializers.deserializeAll(serializedArguments)
    }
}

/**
 * Get a serializer for a type.
 */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T, S> SerializationHandler<S>.getSerializer() = getSerializer(typeOf<T>()) as Serializer<T, S>

/**
 * Get a serializer for a type.
 */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T> SerializationHandler<*>.getMethodSerializer() = MethodSerializer(
    transformer as SerializedFormatTransformer<Any?>,
    getSerializer(typeOf<T>()) as Serializer<T, Any?>
)

/**
 * Get serializers for a method and check that all were gotten.
 */
fun <S> SerializationHandler<S>.getAndCheckSerializers(types: MethodTypes) =
    getSerializers(types).also { types.checkSerializers(it) }
