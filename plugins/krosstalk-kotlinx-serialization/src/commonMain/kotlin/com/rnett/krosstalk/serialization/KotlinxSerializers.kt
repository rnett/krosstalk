package com.rnett.krosstalk.serialization

import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.serialization.plugin.*
import com.rnett.krosstalk.serialization.plugin.Serializer
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.reflect.KType

/**
 * A dynamic heterogeneous composite serializer, i.e. a map, but with a different serializer for each key
 */
internal class ArgumentSerializer(private val serializers: Map<String, KSerializer<Any?>>): KSerializer<Map<String, Any?>> {
    private val order = serializers.keys.toList()
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Arguments"){
        order.forEach { name ->
            val serializer = serializers.getValue(name)
            element(name, serializer.descriptor, isOptional = true)
        }
    }

    override fun deserialize(decoder: Decoder): Map<String, Any?> =
        decoder.decodeStructure(descriptor){
            val map = mutableMapOf<String, Any?>()
            while (true){
                val index = decodeElementIndex(descriptor)

                if(index == CompositeDecoder.DECODE_DONE)
                    break

                val name = order[index]
                val serializer = serializers.getValue(name)

                map[name] = if(name in map){
                    decodeNullableSerializableElement(descriptor, index, serializer, map[name])
                } else {
                    decodeNullableSerializableElement(descriptor, index, serializer)
                }
            }
            map
        }

    override fun serialize(encoder: Encoder, value: Map<String, Any?>) {
        encoder.encodeStructure(descriptor){
            value.forEach {
                encodeNullableSerializableElement(descriptor, order.indexOf(it.key), serializers.getValue(it.key), it.value)
            }
        }
    }
}

@KrosstalkPluginApi
public sealed interface KotlinxSerializer<T, S> : Serializer<T, S>{
    public val serializer: KSerializer<T>
}

/**
 * A serializer that uses a kotlinx [KSerializer] with a [BinaryFormat].
 */
@OptIn(KrosstalkPluginApi::class)
public data class KotlinxBinarySerializer<T>(override val serializer: KSerializer<T>, val format: BinaryFormat) :
    KotlinxSerializer<T, ByteArray> {
    override fun deserialize(data: ByteArray): T = format.decodeFromByteArray(serializer, data)

    override fun serialize(data: T): ByteArray = format.encodeToByteArray(serializer, data)
}


/**
 * A serializer that uses a kotlinx [KSerializer] with a [StringFormat].
 */
@OptIn(KrosstalkPluginApi::class)
public data class KotlinxStringSerializer<T>(override val serializer: KSerializer<T>, val format: StringFormat) :
    KotlinxSerializer<T, String> {
    override fun deserialize(data: String): T = format.decodeFromString(serializer, data)

    override fun serialize(data: T): String = format.encodeToString(serializer, data)
}

/**
 * Kotlinx serialization handler that uses a [BinaryFormat].
 * Serializes arguments as a composite structure, as if they were properties in a class.
 */
@OptIn(KrosstalkPluginApi::class)
public data class KotlinxBinarySerializationHandler(val format: BinaryFormat, override val contentType: String = byteArrayContentType) :
    BaseSerializationHandler<ByteArray>(ByteTransformer) {

    override fun getSerializer(type: KType): KotlinxBinarySerializer<*> =
        KotlinxBinarySerializer(serializer(type), format)

    override fun serializeArguments(arguments: Map<String, *>, serializers: ArgumentSerializers<ByteArray>): ByteArray {
        val kotlinxSerializers = serializers.map.mapValues { (it.value as KotlinxSerializer).serializer }
        val topLevelSerializer = ArgumentSerializer(kotlinxSerializers as Map<String, KSerializer<Any?>>)
        return format.encodeToByteArray(topLevelSerializer, arguments)
    }

    override fun deserializeArguments(
        arguments: ByteArray,
        serializers: ArgumentSerializers<ByteArray>
    ): Map<String, *> {
        val kotlinxSerializers = serializers.map.mapValues { (it.value as KotlinxSerializer).serializer }
        val topLevelSerializer = ArgumentSerializer(kotlinxSerializers as Map<String, KSerializer<Any?>>)
        return format.decodeFromByteArray(topLevelSerializer, arguments)
    }
}


/**
 * Kotlinx serialization handler that uses a [StringFormat].
 * Serializes arguments as a composite structure, as if they were properties in a class.
 */
@OptIn(KrosstalkPluginApi::class)
public data class KotlinxStringSerializationHandler(val format: StringFormat, override val contentType: String = if(format is Json) "application/json" else stringContentType) :
    BaseSerializationHandler<String>(StringTransformer) {

    override fun serializeArguments(arguments: Map<String, *>, serializers: ArgumentSerializers<String>): String {
        val kotlinxSerializers = serializers.map.mapValues { (it.value as KotlinxSerializer).serializer }
        val topLevelSerializer = ArgumentSerializer(kotlinxSerializers as Map<String, KSerializer<Any?>>)
        return format.encodeToString(topLevelSerializer, arguments)
    }

    override fun deserializeArguments(
        arguments: String,
        serializers: ArgumentSerializers<String>
    ): Map<String, *> {
        val kotlinxSerializers = serializers.map.mapValues { (it.value as KotlinxSerializer).serializer }
        val topLevelSerializer = ArgumentSerializer(kotlinxSerializers as Map<String, KSerializer<Any?>>)
        return format.decodeFromString(topLevelSerializer, arguments)
    }

    override fun getSerializer(type: KType): KotlinxStringSerializer<*> =
        KotlinxStringSerializer(serializer(type), format)
}
