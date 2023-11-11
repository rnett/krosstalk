package com.rnett.krosstalk.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import kotlin.reflect.KType

@ExperimentalSerializationApi
internal class StructSerializer(fields: Map<String, KType>, private val module: SerializersModule) :
    KSerializer<Map<String, Any?>> {
    private val fieldSerializers = fields.mapValues { module.serializer(it.value) }
    private val fieldSerializersIndexed = fieldSerializers.entries.withIndex().associate {
        it.index to it.value.toPair()
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ArgumentsMap") {
        fieldSerializers.forEach {
            element(it.key, it.value.descriptor)
        }
    }

    override fun deserialize(decoder: Decoder): Map<String, Any?> {
        return buildMap {
            decoder.decodeStructure(descriptor) {
                if (decodeSequentially()) { // sequential decoding protocol
                    fieldSerializers.onEachIndexed { i, entry ->
                        this@buildMap[entry.key] =
                            decodeNullableSerializableElement(descriptor, i, entry.value, this@buildMap[entry.key])
                    }
                } else while (true) {
                    val index = decodeElementIndex(descriptor)
                    if (index == CompositeDecoder.DECODE_DONE)
                        break

                    val param = fieldSerializersIndexed.getOrElse(index) { error("Unexpected index: $index") }

                    this@buildMap[param.first] =
                        decodeNullableSerializableElement(descriptor, index, param.second, this@buildMap[param.first])
                }
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, Any?>) {
        encoder.encodeStructure(descriptor) {
            fieldSerializers.onEachIndexed { i, entry ->
                encodeNullableSerializableElement(descriptor, i, entry.value, value.getValue(entry.key))
            }
        }
    }
}