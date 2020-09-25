package com.rnett.krosstalk

import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class MethodSerializers<S> @PublishedApi internal constructor(
        val paramSerializers: Map<String, Serializer<*, S>>,
        val resultSerializer: Serializer<*, S>
) {
    val instanceReceiverSerializer by lazy { paramSerializers[instanceParameterKey] }
    val extensionReceiverSerializer by lazy { paramSerializers[extensionParameterKey] }
}

class MethodTypes(
    val paramTypes: Map<String, KType>,
    val resultType: KType
) {
    inline fun <S> toSerializers(getSerializer: (KType) -> Serializer<*, S>) = MethodSerializers(
            paramTypes.mapValues { getSerializer(it.value) },
            getSerializer(resultType)
    )

    val instanceReceiverType by lazy { paramTypes[instanceParameterKey] }
    val extensionReceiverSerializer by lazy { paramTypes[extensionParameterKey] }

    fun checkSerializers(serializers: MethodSerializers<*>) {
        paramTypes.keys.forEach {
            check(it in serializers.paramSerializers) { "Missing serializer for $it" }
        }
    }
}

interface SerializationHandler<S> {
    fun getSerializers(types: MethodTypes): MethodSerializers<S> = types.toSerializers {
        getSerializer(it)
    }

    fun getSerializer(type: KType): Serializer<*, S>
    fun serializeArguments(arguments: Map<String, *>, serializers: Map<String, Serializer<*, S>>): S
    fun deserializeArguments(arguments: S, serializers: Map<String, Serializer<*, S>>): Map<String, *>

    fun toByteArray(data: S): ByteArray
    fun fromByteArray(data: ByteArray): S
}

fun <S> SerializationHandler<S>.castMethodSerializers(methodSerializers: MethodSerializers<*>) = methodSerializers as MethodSerializers<S>

fun <S> SerializationHandler<S>.serializeByteArguments(arguments: Map<String, *>, serializers: Map<String, Serializer<*, S>>): ByteArray = toByteArray(serializeArguments(arguments, serializers))
fun <S> SerializationHandler<S>.deserializeByteArguments(arguments: ByteArray, serializers: Map<String, Serializer<*, S>>): Map<String, *> = deserializeArguments(fromByteArray(arguments), serializers)

fun <S> SerializationHandler<S>.serializeByteArguments(arguments: Map<String, *>, serializers: MethodSerializers<*>): ByteArray = toByteArray(serializeArguments(arguments, castMethodSerializers(serializers).paramSerializers))
fun <S> SerializationHandler<S>.deserializeByteArguments(arguments: ByteArray, serializers: MethodSerializers<*>): Map<String, *> = deserializeArguments(fromByteArray(arguments), castMethodSerializers(serializers).paramSerializers)

//TODO interface default methods don't work in JS IR?  make issue

abstract class StringSerializationHandler : SerializationHandler<String> {
    override fun toByteArray(data: String): ByteArray = data.encodeToByteArray()
    override fun fromByteArray(data: ByteArray): String = data.decodeToString()
}

abstract class StringArgumentSerializationHandler : StringSerializationHandler() {
    abstract fun serializeArguments(serializedArguments: Map<String, String>): String
    abstract fun deserializeArguments(arguments: String): Map<String, String>

    final override fun serializeArguments(arguments: Map<String, *>, serializers: Map<String, Serializer<*, String>>): String {
        val serializedArguments = arguments.mapValues { (serializers.getValue(it.key) as StringSerializer<Any?>).serialize(it.value) }
        return serializeArguments(serializedArguments)
    }

    final override fun deserializeArguments(arguments: String, serializers: Map<String, Serializer<*, String>>): Map<String, *> {
        val serializedArguments = deserializeArguments(arguments)
        return serializedArguments.mapValues {
            (serializers.getValue(it.key) as StringSerializer<Any?>).deserialize(it.value)
        }
    }
}

abstract class BinarySerializationHandler : SerializationHandler<ByteArray> {
    override fun toByteArray(data: ByteArray): ByteArray = data
    override fun fromByteArray(data: ByteArray): ByteArray = data
}

abstract class BinaryArgumentSerializationHandler : BinarySerializationHandler() {
    abstract fun serializeArguments(serializedArguments: Map<String, ByteArray>): ByteArray
    abstract fun deserializeArguments(arguments: ByteArray): Map<String, ByteArray>

    final override fun serializeArguments(arguments: Map<String, *>, serializers: Map<String, Serializer<*, ByteArray>>): ByteArray {
        val serializedArguments = arguments.mapValues { (serializers.getValue(it.key) as BinarySerializer<Any?>).serialize(it.value) }
        return serializeArguments(serializedArguments)
    }

    final override fun deserializeArguments(arguments: ByteArray, serializers: Map<String, Serializer<*, ByteArray>>): Map<String, *> {
        val serializedArguments = deserializeArguments(arguments)
        return serializedArguments.mapValues {
            (serializers.getValue(it.key) as BinarySerializer<Any?>).deserialize(it.value)
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T, S> SerializationHandler<S>.getSerializer() = getSerializer(typeOf<T>()) as Serializer<T, S>

fun <S> SerializationHandler<S>.getAndCheckSerializers(types: MethodTypes) =
        getSerializers(types).also { types.checkSerializers(it) }

interface Serializer<T, S> {
    fun toByteArray(data: S): ByteArray
    fun fromByteArray(data: ByteArray): S

    fun deserialize(data: S): T
    fun serialize(data: T): S
}

fun <T, S> Serializer<T, S>.serializeToBytes(data: T): ByteArray = toByteArray(serialize(data))
fun <T, S> Serializer<T, S>.deserializeFromBytes(data: ByteArray): T = deserialize(fromByteArray(data))

interface StringSerializer<T> : Serializer<T, String> {
    override fun toByteArray(data: String): ByteArray = data.encodeToByteArray()
    override fun fromByteArray(data: ByteArray): String = data.decodeToString()
}

interface BinarySerializer<T> : Serializer<T, ByteArray> {
    override fun toByteArray(data: ByteArray): ByteArray = data
    override fun fromByteArray(data: ByteArray): ByteArray = data
}
