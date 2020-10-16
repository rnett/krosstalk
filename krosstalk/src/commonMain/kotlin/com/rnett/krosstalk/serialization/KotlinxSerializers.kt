package com.rnett.krosstalk.serialization

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import kotlin.reflect.KType

/**
 * A serializer that uses a kotlinx [KSerializer] with a [BinaryFormat].
 */
class KotlinxBinarySerializer<T>(val serializer: KSerializer<T>, val format: BinaryFormat) : BinarySerializer<T> {
    override fun deserialize(data: ByteArray): T = format.decodeFromByteArray(serializer, data)

    override fun serialize(data: T): ByteArray = format.encodeToByteArray(serializer, data)
}


/**
 * A serializer that uses a kotlinx [KSerializer] with a [StringFormat].
 */
class KotlinxStringSerializer<T>(val serializer: KSerializer<T>, val format: StringFormat) : StringSerializer<T> {
    override fun deserialize(data: String): T = format.decodeFromString(serializer, data)

    override fun serialize(data: T): String = format.encodeToString(serializer, data)
}

/**
 * Kotlinx serialization handler that uses a [BinaryFormat].  Combines arguments into a `Map<String, ByteArray>`, then serializes the map.
 */
class KotlinxBinarySerializationHandler(val format: BinaryFormat) : ArgumentSerializationHandler<ByteArray>(ByteTransformer) {
    override fun serializeArguments(serializedArguments: Map<String, ByteArray>): ByteArray {
        return format.encodeToByteArray(mapSerializer, serializedArguments)
    }

    override fun deserializeArguments(arguments: ByteArray): Map<String, ByteArray> {
        return format.decodeFromByteArray(mapSerializer, arguments)
    }

    override fun getSerializer(type: KType) = KotlinxBinarySerializer(serializer(type), format)

    val mapSerializer = serializer<Map<String, ByteArray>>()
}


/**
 * Kotlinx serialization handler that uses a [StringFormat].  Combines arguments into a `Map<String, String>`, then serializes the map.
 *
 * Note that this will result in arguments being wrapped in strings in the final object.
 * If you want the arguments to be objects, which you probably do, use [KotlinxJsonObjectSerializationHandler].
 * This is necessary for using non-krosstalk apis.
 */
class KotlinxStringSerializationHandler(val format: StringFormat) : ArgumentSerializationHandler<String>(StringTransformer) {
    override fun serializeArguments(serializedArguments: Map<String, String>): String {
        return format.encodeToString(mapSerializer, serializedArguments)
    }

    override fun deserializeArguments(arguments: String): Map<String, String> {
        return format.decodeFromString(mapSerializer, arguments)
    }

    override fun getSerializer(type: KType) = KotlinxStringSerializer(serializer(type), format)

    val mapSerializer = serializer<Map<String, String>>()
}

/**
 * Kotlinx json serialization handler that, instead of combining arguments into a `Map<String, String>`, uses them as fields in a json object.
 * If you are interacting with a non-krosstalk api, this is almost certainly what you want to use.
 */
class KotlinxJsonObjectSerializationHandler(val format: Json) : BaseSerializationHandler<String>(StringTransformer) {
    override fun getSerializer(type: KType) = KotlinxStringSerializer(serializer(type), format)

    override fun serializeArguments(arguments: Map<String, *>, serializers: ArgumentSerializers<String>): String {
        val jsonObject = buildJsonObject {
            arguments.forEach { (key, data) ->
                put(key, format.encodeToJsonElement((serializers[key] as KotlinxStringSerializer<Any?>).serializer, data))
            }
        }
        return jsonObject.toString()
    }

    override fun deserializeArguments(arguments: String, serializers: ArgumentSerializers<String>): Map<String, *> {
        val jsonObject = format.parseToJsonElement(arguments).jsonObject
        return jsonObject.mapValues { (key, data) ->
            Json.decodeFromJsonElement((serializers[key] as KotlinxStringSerializer<Any?>).serializer, data)
        }
    }
}