package com.rnett.krosstalk.serialization.plugin

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.serialization.MethodArgumentSerializers
import com.rnett.krosstalk.serialization.MethodTypes
import com.rnett.krosstalk.serialization.TransformedSerializer
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public const val byteArrayContentType: String = "application/octet-stream"
public const val stringContentType: String = "text/plain; charset=utf-8"

/**
 * A SerializationHandler capable of getting serializers from [KType]s,
 * serializing and deserializing argument maps to some intermediate data type [S],
 * and turning [S]s into [ByteArray]s and [String]s and visa versa.
 *
 * Requires manual serialization of the argument maps for support for things like putting them in a JSON object.
 * To have each argument automatically serialized use [StringArgumentSerializationHandler] or [BinaryArgumentSerializationHandler].
 *
 */
@OptIn(KrosstalkPluginApi::class)
public interface SerializationHandler<S> {

    /**
     * Get a serializer for a [KType].
     */
    public fun getSerializer(type: KType): Serializer<*, S>

    /**
     * Serialize a argument map to [S] using the provided argument serializers.
     */
    public fun serializeArguments(arguments: Map<String, *>, serializers: ArgumentSerializers<S>): S

    /**
     * Deserialize an argument map from [S] using the provided argument serializers.
     */
    public fun deserializeArguments(arguments: S, serializers: ArgumentSerializers<S>): Map<String, *>

    /**
     * Serialize a single argument to [S].
     */
    public fun <T> serializeArgument(key: String, value: T, serializers: ArgumentSerializers<S>): S = serializers.serializeArgument(key, value)

    /**
     * Deserialize a single argument from [S].
     */
    public fun <T> deserializeArgument(key: String, value: S, serializers: ArgumentSerializers<S>): T = serializers.deserializeArgument(key, value)

    /**
     * Get the format transformer, to turn the serial format into byte arrays and strings
     */
    public val transformer: SerializedFormatTransformer<S>

    /**
     * Get the default content type fo use for requests.
     */
    public val contentType: String get() = "application/*"
}


@KrosstalkPluginApi
public abstract class BaseSerializationHandler<S>(override val transformer: SerializedFormatTransformer<S>) : SerializationHandler<S>

/**
 * A [StringSerializationHandler] that automatically serializes/deserializes each argument before calling [serializeArguments]/[deserializeArguments].
 */
@KrosstalkPluginApi
public abstract class ArgumentSerializationHandler<S>(transformer: SerializedFormatTransformer<S>) : BaseSerializationHandler<S>(transformer) {
    /**
     * Combine serialized arguments into a final serialized form.
     */
    public abstract fun serializeArguments(serializedArguments: Map<String, S>): S

    /**
     * Deconstruct the final serialized form into serialized arguments.
     */
    public abstract fun deserializeArguments(arguments: S): Map<String, S>

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
@InternalKrosstalkApi
@OptIn(ExperimentalStdlibApi::class, KrosstalkPluginApi::class)
internal inline fun <reified T> SerializationHandler<*>.getMethodSerializer() = TransformedSerializer(
    transformer as SerializedFormatTransformer<Any?>,
    getSerializer(typeOf<T>()) as Serializer<T, Any?>
)

/**
 * Get a serializer for a type.
 */
@InternalKrosstalkApi
@OptIn(ExperimentalStdlibApi::class, KrosstalkPluginApi::class)
internal fun <T> SerializationHandler<*>.getMethodSerializer(type: KType) = TransformedSerializer(
    transformer as SerializedFormatTransformer<Any?>,
    getSerializer(type) as Serializer<T, Any?>
)

@OptIn(KrosstalkPluginApi::class)
@InternalKrosstalkApi
internal fun <S> SerializationHandler<S>.getArgumentSerializers(types: MethodTypes) =
    MethodArgumentSerializers(this, ArgumentSerializers(types.paramTypes.mapValues { this.getSerializer(it.value) }))
