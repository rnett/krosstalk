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

data class MethodTypes(
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
}

interface SerializationHandler {
    fun getSerializers(types: MethodTypes): MethodSerializers
}

interface Serializer<T> {
    fun deserialize(data: ByteArray): T
    fun serialize(data: T): ByteArray
}

class KotlinxSerializer<T>(val serializer: KSerializer<T>) : Serializer<T> {
    override fun deserialize(data: ByteArray): T {
        return Cbor.load(serializer, data)
    }

    override fun serialize(data: T): ByteArray {
        return Cbor.dump(serializer, data)
    }

}

data class KotlinxSerializers(val paramSerializers: Map<String, KSerializer<*>>, val returnSerializer: KSerializer<*>)

object KotlinxSerializationHandler : SerializationHandler {
    override fun getSerializers(types: MethodTypes): MethodSerializers = types.toSerializers { KotlinxSerializer(serializer(it)) }
}