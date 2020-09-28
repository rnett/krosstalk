package com.rnett.krosstalk

import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * All necessary serializers for a method.
 * Instance and extension receiver serializers are in the [paramSerializers] map with keys [instanceParameterKey] and [extensionParameterKey], respectively, but can be accessed through their accessors.
 */
data class MethodSerializers<S> @PublishedApi internal constructor(
        val paramSerializers: ArgumentSerializers<S>,
        val resultSerializer: Serializer<*, S>
) {
    val instanceReceiverSerializer by lazy { paramSerializers.map[instanceParameterKey] }
    val extensionReceiverSerializer by lazy { paramSerializers.map[extensionParameterKey] }
}

/**
 * All necessary types for a method.
 * Instance and extension receiver serializers are in the [paramTypes] map with keys [instanceParameterKey] and [extensionParameterKey], respectively, but can be accessed through their accessors.
 */
class MethodTypes(
        val paramTypes: Map<String, KType>,
        val resultType: KType
) {
    inline fun <S> toSerializers(getSerializer: (KType) -> Serializer<*, S>) = MethodSerializers(
            ArgumentSerializers(paramTypes.mapValues { getSerializer(it.value) }),
            getSerializer(resultType)
    )

    val instanceReceiverType by lazy { paramTypes[instanceParameterKey] }
    val extensionReceiverSerializer by lazy { paramTypes[extensionParameterKey] }

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
    operator fun get(argument: String) = (map[argument]
            ?: throw KrosstalkException.MissingSerializer(argument, map.keys)) as Serializer<Any?, S>

    /**
     * Serialize all arguments, throwing [KrosstalkException.MissingSerializer] if a serializer is missing.
     */
    fun serializeAll(arguments: Map<String, *>) = arguments.mapValues { this[it.key].serialize(it.value) }

    /**
     * Deserialize all arguments, throwing [KrosstalkException.MissingSerializer] if a serializer is missing.
     */
    fun deserializeAll(arguments: Map<String, S>) = arguments.mapValues { this[it.key].deserialize(it.value) }

}

/**
 * A SerializationHandler capable of getting serializers from [KType]s,
 * serializing and deserializing argument maps to some intermediate data type [S],
 * and turning [S]s into [ByteArray]s and visa versa.
 *
 * Requires manual serialization of the argument maps for support for things like putting them in a JSON object.
 * To have each argument automatically serialized use [StringArgumentSerializationHandler] or [BinaryArgumentSerializationHandler].
 *
 */
interface SerializationHandler<S> {
    /**
     * Get a [MethodSerializers] from a [MethodTypes].  By default uses [MethodTypes.toSerializers] with [getSerializer].
     */
    fun getSerializers(types: MethodTypes): MethodSerializers<S> = types.toSerializers {
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
     * Transform [S] to a [ByteArray].
     */
    fun toByteArray(data: S): ByteArray

    /**
     * Transform a [ByteArray] to [S].
     */
    fun fromByteArray(data: ByteArray): S
}

/**
 * Helper method to cast a `MethodSerializers<*>` to a [MethodSerializers] with the type required by this [SerializationHandler].  **Preforms no checking, should only be used when you know the type matches.**
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Preforms no checking, should only be used when you know the type matches.")
@PublishedApi
internal fun <S> SerializationHandler<S>.castMethodSerializers(methodSerializers: MethodSerializers<*>) = methodSerializers as MethodSerializers<S>

/**
 * Serialize arguments to a [ByteArray] by serializing them to [S] and then using [SerializationHandler.toByteArray].
 */
fun <S> SerializationHandler<S>.serializeByteArguments(arguments: Map<String, *>, serializers: ArgumentSerializers<S>): ByteArray = toByteArray(serializeArguments(arguments, serializers))

/**
 * Deserialize arguments from a [ByteArray] by using [SerializationHandler.fromByteArray] then deserializing them.
 */
fun <S> SerializationHandler<S>.deserializeByteArguments(arguments: ByteArray, serializers: ArgumentSerializers<S>): Map<String, *> = deserializeArguments(fromByteArray(arguments), serializers)

@PublishedApi
internal fun <S> SerializationHandler<S>.serializeByteArguments(arguments: Map<String, *>, serializers: MethodSerializers<*>): ByteArray = toByteArray(serializeArguments(arguments, castMethodSerializers(serializers).paramSerializers))

@PublishedApi
internal fun <S> SerializationHandler<S>.deserializeByteArguments(arguments: ByteArray, serializers: MethodSerializers<*>): Map<String, *> = deserializeArguments(fromByteArray(arguments), castMethodSerializers(serializers).paramSerializers)

//TODO interface default methods don't work in JS IR?  make issue

/**
 * A [SerializationHandler] that serializes to [String].
 * Defines [SerializationHandler.toByteArray] and [SerializationHandler.fromByteArray] for you.
 */
abstract class StringSerializationHandler : SerializationHandler<String> {
    override fun toByteArray(data: String): ByteArray = data.encodeToByteArray()
    override fun fromByteArray(data: ByteArray): String = data.decodeToString()
}

/**
 * A [StringSerializationHandler] that automatically serializes/deserializes each argument before calling [serializeArguments]/[deserializeArguments].
 */
abstract class StringArgumentSerializationHandler : StringSerializationHandler() {
    /**
     * Combine serialized arguments into a final serialized form.
     */
    abstract fun serializeArguments(serializedArguments: Map<String, String>): String

    /**
     * Deconstruct the final serialized form into serialized arguments.
     */
    abstract fun deserializeArguments(arguments: String): Map<String, String>

    final override fun serializeArguments(arguments: Map<String, *>, serializers: ArgumentSerializers<String>): String {
        return serializeArguments(serializers.serializeAll(arguments))
    }

    final override fun deserializeArguments(arguments: String, serializers: ArgumentSerializers<String>): Map<String, *> {
        val serializedArguments = deserializeArguments(arguments)
        return serializers.deserializeAll(serializedArguments)
    }
}


/**
 * A [SerializationHandler] that serializes directly to [ByteArray].
 * Defines [SerializationHandler.toByteArray] and [SerializationHandler.fromByteArray] for you.
 */
abstract class BinarySerializationHandler : SerializationHandler<ByteArray> {
    override fun toByteArray(data: ByteArray): ByteArray = data
    override fun fromByteArray(data: ByteArray): ByteArray = data
}


/**
 * A [BinarySerializationHandler] that automatically serializes/deserializes each argument before calling [serializeArguments]/[deserializeArguments].
 */
abstract class BinaryArgumentSerializationHandler : BinarySerializationHandler() {
    /**
     * Combine serialized arguments into a final serialized form.
     */
    abstract fun serializeArguments(serializedArguments: Map<String, ByteArray>): ByteArray

    /**
     * Deconstruct the final serialized form into serialized arguments.
     */
    abstract fun deserializeArguments(arguments: ByteArray): Map<String, ByteArray>

    final override fun serializeArguments(arguments: Map<String, *>, serializers: ArgumentSerializers<ByteArray>): ByteArray {
        val serializedArguments = serializers.serializeAll(arguments)
        return serializeArguments(serializedArguments)
    }

    final override fun deserializeArguments(arguments: ByteArray, serializers: ArgumentSerializers<ByteArray>): Map<String, *> {
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
 * Get serializers for a method and check that all were gotten.
 */
fun <S> SerializationHandler<S>.getAndCheckSerializers(types: MethodTypes) =
        getSerializers(types).also { types.checkSerializers(it) }

/**
 * A serializer/deserializer that transforms [T] to and from [S] and visa versa, and transforms [S] to and from [ByteArray].
 */
interface Serializer<T, S> {
    /**
     * Transform [S] into a [ByteArray].
     */
    fun toByteArray(data: S): ByteArray

    /**
     * Transform a [ByteArray] into [S].
     */
    fun fromByteArray(data: ByteArray): S

    /**
     * Deserialize [T] from [S].
     */
    fun deserialize(data: S): T

    /**
     * Serialize [T] to [S].
     */
    fun serialize(data: T): S
}

/**
 * Serialize [data] directly to a [ByteArray].
 */
fun <T, S> Serializer<T, S>.serializeToBytes(data: T): ByteArray = toByteArray(serialize(data))

/**
 * Deserialize [T] from a [ByteArray].
 */
fun <T, S> Serializer<T, S>.deserializeFromBytes(data: ByteArray): T = deserialize(fromByteArray(data))

/**
 * A serializer that serializes/deserializes to/from [String].
 */
interface StringSerializer<T> : Serializer<T, String> {
    override fun toByteArray(data: String): ByteArray = data.encodeToByteArray()
    override fun fromByteArray(data: ByteArray): String = data.decodeToString()
}


/**
 * A serializer that directly serializes/deserializes to/from [ByteArray].
 */
interface BinarySerializer<T> : Serializer<T, ByteArray> {
    override fun toByteArray(data: ByteArray): ByteArray = data
    override fun fromByteArray(data: ByteArray): ByteArray = data
}
