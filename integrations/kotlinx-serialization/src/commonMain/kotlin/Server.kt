package com.rnett.krosstalk.serialization

import com.rnett.krosstalk.metadata.ParameterType
import com.rnett.krosstalk.server.KrosstalkServerSerialization
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import kotlin.reflect.KType

public class KotlinxServerSerialization(private val format: BinaryFormat) :
    KrosstalkServerSerialization {
    override fun deserialize(parameters: Map<String, ParameterType>, data: ByteArray): Map<String, Any?> {
        return format.decodeFromByteArray(ArgumentDeserializer(parameters), data)
    }

    override fun serialize(data: Any?, type: KType): ByteArray {
        return format.encodeToByteArray(format.serializersModule.serializer(type), data)
    }
}

public fun BinaryFormat.krosstalkServerSerialization(): KotlinxServerSerialization = KotlinxServerSerialization(this)

internal class TypedMapSerializer(parameters: Map<String, KType>, private val module: SerializersModule) :
    KSerializer<Map<String, Any?>> {
    private val parameterSerializers = parameters.mapValues { module.serializer(it.value) }
    private val parameterSerializersIndexed = parameterSerializers.entries.withIndex().associate {
        it.index to it.value.toPair()
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ArgumentsMap") {
        parameterSerializers.forEach {
            element(it.key, it.value.descriptor)
        }
    }

    override fun deserialize(decoder: Decoder): Map<String, Any?> {
        return buildMap {
            decoder.decodeStructure(descriptor) {
                if (decodeSequentially()) { // sequential decoding protocol
                    parameterSerializers.onEachIndexed { i, entry ->
                        this@buildMap[entry.key] =
                            decodeNullableSerializableElement(descriptor, i, entry.value, this@buildMap[entry.key])
                    }
                } else while (true) {
                    val index = decodeElementIndex(descriptor)
                    if (index == CompositeDecoder.DECODE_DONE)
                        break

                    val param = parameterSerializersIndexed.getOrElse(index) { error("Unexpected index: $index") }

                    this@buildMap[param.first] =
                        decodeNullableSerializableElement(descriptor, index, param.second, this@buildMap[param.first])
                }
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, Any?>) {
        encoder.encodeStructure(descriptor) {
            parameterSerializers.onEachIndexed { i, entry ->
                encodeNullableSerializableElement(descriptor, i, entry.value, value.getValue(entry.key))
            }
        }
    }
}