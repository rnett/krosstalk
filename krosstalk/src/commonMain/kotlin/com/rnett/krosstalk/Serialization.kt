package com.rnett.krosstalk

import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.serializer
import kotlin.reflect.KType


data class MethodSerializers(
        val paramSerializers: Map<String, Serializer<*>>,
        val instanceReceiverSerializer: Serializer<*>?,
        val extensionReceiverSerializer: Serializer<*>?,
        val resultSerializer: Serializer<*>
)

class MethodTypes(
        val paramTypes: Map<String, KType>,
        val resultType: KType,
        val instanceReceiverType: KType? = null,
        val extensionReceiverType: KType? = null
) {
    inline fun toSerializers(getSerializer: (KType) -> Serializer<*>) = MethodSerializers(
            paramTypes.mapValues { getSerializer(it.value) },
            instanceReceiverType?.let(getSerializer),
            extensionReceiverType?.let(getSerializer),
            getSerializer(resultType)
    )

    fun checkSerializers(serializers: MethodSerializers) {
        paramTypes.keys.forEach {
            check(it in serializers.paramSerializers) { "Missing serializer for $it" }
        }

        if (instanceReceiverType != null)
            check(serializers.instanceReceiverSerializer != null) { "Missing serializer for instance receiver" }

        if (extensionReceiverType != null)
            check(serializers.extensionReceiverSerializer != null) { "Missing serializer for extension receiver" }
    }
}

interface SerializationHandler {
    fun getSerializers(types: MethodTypes): MethodSerializers
}

fun SerializationHandler.getAndCheckSerializers(types: MethodTypes) =
    getSerializers(types).also { types.checkSerializers(it) }

interface Serializer<T> {
    fun deserialize(data: ByteArray): T
    fun serialize(data: T): ByteArray
}

class KotlinxSerializer<T>(val serializer: KSerializer<T>) : Serializer<T> {
    override fun deserialize(data: ByteArray): T {
        return Cbor.decodeFromByteArray(serializer, data)
    }

    override fun serialize(data: T): ByteArray {
        return Cbor.encodeToByteArray(serializer, data)
    }

}

data class KotlinxSerializers(val paramSerializers: Map<String, KSerializer<*>>, val returnSerializer: KSerializer<*>)

object KotlinxSerializationHandler : SerializationHandler {
    override fun getSerializers(types: MethodTypes): MethodSerializers = types.toSerializers { KotlinxSerializer(serializer(it)) }
}